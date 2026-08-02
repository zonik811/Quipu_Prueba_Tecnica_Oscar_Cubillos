package com.playlist.spotifyplaylistapi.controller;

import com.playlist.spotifyplaylistapi.model.Playlist;
import com.playlist.spotifyplaylistapi.model.Song;
import com.playlist.spotifyplaylistapi.model.dto.RecommendationResponse;
import com.playlist.spotifyplaylistapi.model.dto.RecommendationResponse.RecommendationItem;
import com.playlist.spotifyplaylistapi.repository.PlaylistRepository;
import com.playlist.spotifyplaylistapi.security.JwtFilter;
import com.playlist.spotifyplaylistapi.security.JwtUtil;
import com.playlist.spotifyplaylistapi.service.IARecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlaylistRepository playlistRepository;

    @MockBean
    private IARecommendationService iaService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtFilter jwtFilter;

    @Test
    void getRecommendations_shouldReturn200() throws Exception {
        Playlist playlist = new Playlist("Rock", "Best rock");
        playlist.addSong(new Song("Bohemian Rhapsody", "Queen", "Album", "1975", "rock"));

        RecommendationResponse response = new RecommendationResponse("Rock",
                List.of(new RecommendationItem("Born to Run", "Bruce Springsteen", "Mismo genero rock")));

        when(playlistRepository.findByNombre("Rock")).thenReturn(Optional.of(playlist));
        when(iaService.recommend(any())).thenReturn(response);

        mockMvc.perform(get("/lists/Rock/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playlist").value("Rock"))
                .andExpect(jsonPath("$.recommendations[0].titulo").value("Born to Run"));
    }

    @Test
    void getRecommendations_shouldReturn404_whenPlaylistNotFound() throws Exception {
        when(playlistRepository.findByNombre("Nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/lists/Nonexistent/recommendations"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecommendations_shouldReturn503_whenIAFails() throws Exception {
        Playlist playlist = new Playlist("Rock", "Best rock");
        playlist.addSong(new Song("Bohemian Rhapsody", "Queen", "Album", "1975", "rock"));

        when(playlistRepository.findByNombre("Rock")).thenReturn(Optional.of(playlist));
        when(iaService.recommend(any())).thenThrow(new RuntimeException("Groq API error"));

        mockMvc.perform(get("/lists/Rock/recommendations"))
                .andExpect(status().isServiceUnavailable());
    }
}
