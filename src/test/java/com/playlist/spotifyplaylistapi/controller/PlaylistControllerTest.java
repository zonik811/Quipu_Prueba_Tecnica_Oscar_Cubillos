package com.playlist.spotifyplaylistapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playlist.spotifyplaylistapi.model.dto.PlaylistRequest;
import com.playlist.spotifyplaylistapi.model.dto.PlaylistResponse;
import com.playlist.spotifyplaylistapi.model.dto.SongDto;
import com.playlist.spotifyplaylistapi.security.JwtFilter;
import com.playlist.spotifyplaylistapi.security.JwtUtil;
import com.playlist.spotifyplaylistapi.service.PlaylistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlaylistController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlaylistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlaylistService playlistService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtFilter jwtFilter;

    @Test
    void createPlaylist_shouldReturn201() throws Exception {
        PlaylistRequest request = new PlaylistRequest();
        request.setNombre("Test List");
        request.setDescripcion("Test Description");
        request.setCanciones(List.of(new SongDto("Title", "Artist", "Album", "2024", "rock")));

        PlaylistResponse response = new PlaylistResponse("Test List", "Test Description",
                List.of(new SongDto("Title", "Artist", "Album", "2024", "rock")));

        when(playlistService.createPlaylist(any())).thenReturn(response);

        mockMvc.perform(post("/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Test List"));
    }

    @Test
    void createPlaylist_withInvalidName_shouldReturn400() throws Exception {
        PlaylistRequest request = new PlaylistRequest();
        request.setNombre(null);
        request.setDescripcion("Test");

        mockMvc.perform(post("/lists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPlaylistByName_shouldReturn200() throws Exception {
        PlaylistResponse response = new PlaylistResponse("Test List", "Desc", List.of());
        when(playlistService.getPlaylistByName("Test List")).thenReturn(Optional.of(response));

        mockMvc.perform(get("/lists/Test List"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Test List"));
    }

    @Test
    void getPlaylistByName_shouldReturn404_whenNotFound() throws Exception {
        when(playlistService.getPlaylistByName("Nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/lists/Nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePlaylist_shouldReturn204() throws Exception {
        when(playlistService.deletePlaylist("Test List")).thenReturn(true);

        mockMvc.perform(delete("/lists/Test List"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePlaylist_shouldReturn404_whenNotFound() throws Exception {
        when(playlistService.deletePlaylist("Nonexistent")).thenReturn(false);

        mockMvc.perform(delete("/lists/Nonexistent"))
                .andExpect(status().isNotFound());
    }
}
