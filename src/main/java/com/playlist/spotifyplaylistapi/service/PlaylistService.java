package com.playlist.spotifyplaylistapi.service;

import com.playlist.spotifyplaylistapi.model.Playlist;
import com.playlist.spotifyplaylistapi.model.Song;
import com.playlist.spotifyplaylistapi.model.dto.PlaylistRequest;
import com.playlist.spotifyplaylistapi.model.dto.PlaylistResponse;
import com.playlist.spotifyplaylistapi.model.dto.SongDto;
import com.playlist.spotifyplaylistapi.repository.PlaylistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PlaylistService {

    private final PlaylistRepository playlistRepository;

    public PlaylistService(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    @Transactional
    public PlaylistResponse createPlaylist(PlaylistRequest request) {
        Playlist playlist = new Playlist(request.getNombre(), request.getDescripcion());

        if (request.getCanciones() != null) {
            for (SongDto songDto : request.getCanciones()) {
                String genero = songDto.getGenero();
                if (genero == null || genero.isBlank()) {
                    genero = "Desconocido";
                }
                Song song = new Song(
                        songDto.getTitulo(),
                        songDto.getArtista(),
                        songDto.getAlbum(),
                        songDto.getAnno(),
                        genero
                );
                playlist.addSong(song);
            }
        }

        playlist = playlistRepository.save(playlist);
        return toResponse(playlist);
    }

    public List<PlaylistResponse> getAllPlaylists() {
        return playlistRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public Optional<PlaylistResponse> getPlaylistByName(String name) {
        return playlistRepository.findByNombre(name)
                .map(this::toResponse);
    }

    @Transactional
    public boolean deletePlaylist(String name) {
        if (playlistRepository.existsByNombre(name)) {
            playlistRepository.deleteByNombre(name);
            return true;
        }
        return false;
    }

    private PlaylistResponse toResponse(Playlist playlist) {
        List<SongDto> songs = playlist.getCanciones().stream()
                .map(s -> new SongDto(s.getTitulo(), s.getArtista(), s.getAlbum(), s.getAnno(), s.getGenero()))
                .collect(Collectors.toList());
        return new PlaylistResponse(playlist.getNombre(), playlist.getDescripcion(), songs);
    }
}
