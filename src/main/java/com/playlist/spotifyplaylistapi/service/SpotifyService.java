package com.playlist.spotifyplaylistapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class SpotifyService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyService.class);
    private static final List<String> DEFAULT_GENRES = List.of(
            "rock", "pop", "jazz", "blues", "metal", "punk", "reggae",
            "hip-hop", "electronic", "classical", "folk", "country", "soul",
            "funk", "disco", "alternative", "indie", "latin", "r-n-b"
    );

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String accessToken;
    private volatile long tokenExpiry;

    public SpotifyService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String obtenerGeneroPorCancion(String titulo, String artista) {
        try {
            String token = getAccessToken();
            String artistId = searchArtistId(token, titulo, artista);
            if (artistId != null) {
                List<String> genres = getArtistGenres(token, artistId);
                if (!genres.isEmpty()) {
                    return String.join(", ", genres);
                }
            }
        } catch (Exception e) {
            log.warn("Error buscando track en Spotify: {}", e.getMessage());
        }

        try {
            String token = getAccessToken();
            List<String> genreSeeds = getAvailableGenreSeeds(token);
            if (!genreSeeds.isEmpty()) {
                return genreSeeds.get(random.nextInt(genreSeeds.size()));
            }
        } catch (Exception e) {
            log.warn("Error obteniendo genre seeds de Spotify: {}", e.getMessage());
        }

        return DEFAULT_GENRES.get(random.nextInt(DEFAULT_GENRES.size()));
    }

    public List<String> obtenerGenerosDisponibles() {
        try {
            String token = getAccessToken();
            List<String> genreSeeds = getAvailableGenreSeeds(token);
            if (!genreSeeds.isEmpty()) {
                return genreSeeds;
            }
        } catch (Exception e) {
            log.warn("Error obteniendo genre seeds: {}", e.getMessage());
        }
        return DEFAULT_GENRES;
    }

    private String searchArtistId(String token, String titulo, String artista) {
        try {
            String query = URLEncoder.encode("track:" + titulo + " artist:" + artista, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.spotify.com/v1/search?q=" + query + "&type=track&limit=1",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("tracks").path("items");

            if (items.isArray() && items.size() > 0) {
                JsonNode artists = items.get(0).path("artists");
                if (artists.isArray() && artists.size() > 0) {
                    return artists.get(0).path("id").asText(null);
                }
            }
        } catch (Exception e) {
            log.debug("Error searchArtistId: {}", e.getMessage());
        }
        return null;
    }

    private List<String> getArtistGenres(String token, String artistId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.spotify.com/v1/artists/" + artistId,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode genresNode = root.path("genres");

            if (genresNode.isArray()) {
                return StreamSupport.stream(genresNode.spliterator(), false)
                        .map(g -> g.asText(null))
                        .filter(g -> g != null)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.debug("Error getArtistGenres: {}", e.getMessage());
        }
        return List.of();
    }

    private List<String> getAvailableGenreSeeds(String token) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.spotify.com/v1/recommendations/available-genre-seeds",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                return List.of();
            }

            JsonNode node = objectMapper.readTree(response.getBody());
            JsonNode genresNode = node.path("genres");
            List<String> genres = new ArrayList<>();
            if (genresNode.isArray()) {
                for (JsonNode genre : genresNode) {
                    genres.add(genre.asText());
                }
            }
            return genres;
        } catch (Exception e) {
            log.debug("Error getAvailableGenreSeeds: {}", e.getMessage());
        }
        return List.of();
    }

    private String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return accessToken;
        }

        tokenLock.lock();
        try {
            if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
                return accessToken;
            }

            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + encodedAuth);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://accounts.spotify.com/api/token",
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Spotify token endpoint returned " + response.getStatusCode());
            }

            JsonNode node = objectMapper.readTree(response.getBody());
            accessToken = node.path("access_token").asText();
            int expiresIn = node.path("expires_in").asInt(3600);
            tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000L) - 60000L;

            return accessToken;
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener token de Spotify", e);
        } finally {
            tokenLock.unlock();
        }
    }
}
