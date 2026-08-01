package com.playlist.spotifyplaylistapi.model.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class PlaylistRequest {

    @NotBlank(message = "El nombre no puede estar vacío")
    private String nombre;

    private String descripcion;

    private List<SongDto> canciones;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<SongDto> getCanciones() {
        return canciones;
    }

    public void setCanciones(List<SongDto> canciones) {
        this.canciones = canciones;
    }
}
