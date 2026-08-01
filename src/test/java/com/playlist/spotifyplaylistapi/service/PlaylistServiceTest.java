package com.playlist.spotifyplaylistapi.service;

import com.playlist.spotifyplaylistapi.model.Playlist;
import com.playlist.spotifyplaylistapi.model.Song;
import com.playlist.spotifyplaylistapi.model.dto.PlaylistRequest;
import com.playlist.spotifyplaylistapi.model.dto.PlaylistResponse;
import com.playlist.spotifyplaylistapi.model.dto.SongDto;
import com.playlist.spotifyplaylistapi.repository.PlaylistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private SpotifyService spotifyService;

    @InjectMocks
    private PlaylistService playlistService;

    private PlaylistRequest request;
    private Playlist playlist;

    @BeforeEach
    void setUp() {
        SongDto songDto = new SongDto("Bohemian Rhapsody", "Queen", "A Night at the Opera", "1975", "rock");

        request = new PlaylistRequest();
        request.setNombre("Rock Classics");
        request.setDescripcion("Best rock songs");
        request.setCanciones(List.of(songDto));

        playlist = new Playlist("Rock Classics", "Best rock songs");
        Song song = new Song("Bohemian Rhapsody", "Queen", "A Night at the Opera", "1975", "rock");
        playlist.addSong(song);
    }

    @Test
    void createPlaylist_shouldReturnPlaylistResponse() {
        when(playlistRepository.save(any(Playlist.class))).thenReturn(playlist);

        PlaylistResponse response = playlistService.createPlaylist(request);

        assertNotNull(response);
        assertEquals("Rock Classics", response.getNombre());
        assertEquals(1, response.getCanciones().size());
        verify(playlistRepository, times(1)).save(any(Playlist.class));
    }

    @Test
    void getAllPlaylists_shouldReturnList() {
        when(playlistRepository.findAll()).thenReturn(List.of(playlist));

        List<PlaylistResponse> responses = playlistService.getAllPlaylists();

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void getPlaylistByName_shouldReturnPlaylist_whenFound() {
        when(playlistRepository.findByNombre("Rock Classics")).thenReturn(Optional.of(playlist));

        Optional<PlaylistResponse> response = playlistService.getPlaylistByName("Rock Classics");

        assertTrue(response.isPresent());
        assertEquals("Rock Classics", response.get().getNombre());
    }

    @Test
    void getPlaylistByName_shouldReturnEmpty_whenNotFound() {
        when(playlistRepository.findByNombre("Nonexistent")).thenReturn(Optional.empty());

        Optional<PlaylistResponse> response = playlistService.getPlaylistByName("Nonexistent");

        assertFalse(response.isPresent());
    }

    @Test
    void deletePlaylist_shouldReturnTrue_whenExists() {
        when(playlistRepository.existsByNombre("Rock Classics")).thenReturn(true);
        doNothing().when(playlistRepository).deleteByNombre("Rock Classics");

        boolean result = playlistService.deletePlaylist("Rock Classics");

        assertTrue(result);
        verify(playlistRepository, times(1)).deleteByNombre("Rock Classics");
    }

    @Test
    void deletePlaylist_shouldReturnFalse_whenNotExists() {
        when(playlistRepository.existsByNombre("Nonexistent")).thenReturn(false);

        boolean result = playlistService.deletePlaylist("Nonexistent");

        assertFalse(result);
        verify(playlistRepository, never()).deleteByNombre(any());
    }

    @Test
    void createPlaylist_withoutGenre_shouldCallSpotifyService() {
        SongDto songDto = new SongDto("Test Song", "Test Artist", "Test Album", "2024", null);
        request.setCanciones(List.of(songDto));

        when(spotifyService.obtenerGeneroPorCancion("Test Song", "Test Artist")).thenReturn("pop");
        when(playlistRepository.save(any(Playlist.class))).thenReturn(playlist);

        PlaylistResponse response = playlistService.createPlaylist(request);

        verify(spotifyService, times(1)).obtenerGeneroPorCancion("Test Song", "Test Artist");
    }
}
