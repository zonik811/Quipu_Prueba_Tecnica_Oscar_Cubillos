package com.playlist.spotifyplaylistapi.controller;

import com.playlist.spotifyplaylistapi.model.Playlist;
import com.playlist.spotifyplaylistapi.model.dto.RecommendationResponse;
import com.playlist.spotifyplaylistapi.repository.PlaylistRepository;
import com.playlist.spotifyplaylistapi.service.IARecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/lists")
public class RecommendationController {

    private static final Logger log = LoggerFactory.getLogger(RecommendationController.class);

    private final PlaylistRepository playlistRepository;
    private final IARecommendationService iaService;

    public RecommendationController(PlaylistRepository playlistRepository,
                                     IARecommendationService iaService) {
        this.playlistRepository = playlistRepository;
        this.iaService = iaService;
    }

    @GetMapping("/{listName}/recommendations")
    public ResponseEntity<?> getRecommendations(@PathVariable String listName) {
        Optional<Playlist> playlistOpt = playlistRepository.findByNombre(listName);

        if (playlistOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorBody(HttpStatus.NOT_FOUND, "Playlist no encontrada"));
        }

        Playlist playlist = playlistOpt.get();

        try {
            RecommendationResponse response = iaService.recommend(playlist);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Recomendaciones no disponibles", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(errorBody(HttpStatus.SERVICE_UNAVAILABLE,
                            "Recomendaciones no disponibles. Intente de nuevo."));
        }
    }

    private Map<String, Object> errorBody(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }
}
