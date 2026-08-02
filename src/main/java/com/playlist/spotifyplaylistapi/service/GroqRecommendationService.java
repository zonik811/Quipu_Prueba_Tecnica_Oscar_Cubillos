package com.playlist.spotifyplaylistapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.spotifyplaylistapi.model.Playlist;
import com.playlist.spotifyplaylistapi.model.Song;
import com.playlist.spotifyplaylistapi.model.dto.RecommendationResponse;
import com.playlist.spotifyplaylistapi.model.dto.RecommendationResponse.RecommendationItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Primary
public class GroqRecommendationService implements IARecommendationService {

    private static final Logger log = LoggerFactory.getLogger(GroqRecommendationService.class);

    private static final String PROMPT_TEMPLATE = """
            Eres un experto musical y curador de playlists. Dada la siguiente lista de canciones,
            recomienda exactamente 5 canciones que NO estén en la lista pero que sean del mismo
            estilo, género, época o artista similar.

            Playlist actual:
            %s

            Reglas:
            - NO repitas ninguna canción de la lista.
            - Recomienda canciones reales que existan (verifica mentalmente).
            - Para cada recomendación, incluye una breve razón (máximo 10 palabras en español)
              explicando por qué encaja (mismo género, misma época, artista relacionado,
              estilo similar, etc.).

            Responde ÚNICAMENTE en este formato JSON exacto, sin texto adicional:
            {"recomendaciones": [{"titulo": "...", "artista": "...", "razon": "..."}]}
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Random random = new Random();
    private final MockRecommendationService mockService;

    @Value("${groq.api-key:${GROQ_API_KEY:}}")
    private String apiKey;

    public GroqRecommendationService(MockRecommendationService mockService) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
        this.mockService = mockService;
    }

    @Override
    public RecommendationResponse recommend(Playlist playlist) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GROQ_API_KEY no configurada. Usando fallback.");
            return mockService.recommend(playlist);
        }

        try {
            List<Song> songs = sampleSongs(playlist.getCanciones(), 20);
            String prompt = buildPrompt(songs);
            String llmResponse = callGroqApi(prompt);
            return parseResponse(playlist.getNombre(), llmResponse);
        } catch (Exception e) {
            log.warn("Groq falló, usando fallback: {}", e.getMessage());
            return mockService.recommend(playlist);
        }
    }

    private List<Song> sampleSongs(List<Song> songs, int max) {
        if (songs.size() <= max) {
            return songs;
        }
        List<Song> copy = new ArrayList<>(songs);
        Collections.shuffle(copy, random);
        return copy.subList(0, max);
    }

    private String buildPrompt(List<Song> songs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < songs.size(); i++) {
            Song s = songs.get(i);
            sb.append(String.format("%d. \"%s\" - %s [%s]\n",
                    i + 1, s.getTitulo(), s.getArtista(),
                    s.getGenero() != null ? s.getGenero() : "desconocido"));
        }
        return String.format(PROMPT_TEMPLATE, sb.toString());
    }

    private String callGroqApi(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", "llama-3.1-8b-instant");
        body.put("temperature", 0.7);
        body.put("max_tokens", 1024);

        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", "Eres un curador musical experto. Responde solo con JSON válido.");

        Map<String, String> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        body.put("messages", List.of(systemMsg, userMsg));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.groq.com/openai/v1/chat/completions",
                request,
                String.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Groq returned " + response.getStatusCode());
        }

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Error parsing Groq response", e);
        }
    }

    private RecommendationResponse parseResponse(String playlistName, String llmContent) {
        try {
            String json = extractJson(llmContent);
            JsonNode root = objectMapper.readTree(json);
            JsonNode recs = root.path("recomendaciones");

            if (!recs.isArray() || recs.size() == 0) {
                throw new RuntimeException("Respuesta sin recomendaciones");
            }

            List<RecommendationItem> items = new ArrayList<>();
            for (JsonNode node : recs) {
                items.add(new RecommendationItem(
                        node.path("titulo").asText("Desconocido"),
                        node.path("artista").asText("Desconocido"),
                        node.path("razon").asText("Encaja con el estilo de la playlist")
                ));
            }
            return new RecommendationResponse(playlistName, items);
        } catch (Exception e) {
            log.warn("Error parseando respuesta de Groq: {}", e.getMessage());
            throw new RuntimeException("JSON inválido del LLM", e);
        }
    }

    private String extractJson(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }
}
