package com.playlist.spotifyplaylistapi.model.dto;

import java.util.List;

public class RecommendationResponse {

    private String playlist;
    private List<RecommendationItem> recommendations;

    public RecommendationResponse() {
    }

    public RecommendationResponse(String playlist, List<RecommendationItem> recommendations) {
        this.playlist = playlist;
        this.recommendations = recommendations;
    }

    public String getPlaylist() {
        return playlist;
    }

    public void setPlaylist(String playlist) {
        this.playlist = playlist;
    }

    public List<RecommendationItem> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<RecommendationItem> recommendations) {
        this.recommendations = recommendations;
    }

    public static class RecommendationItem {
        private String titulo;
        private String artista;
        private String razon;

        public RecommendationItem() {
        }

        public RecommendationItem(String titulo, String artista, String razon) {
            this.titulo = titulo;
            this.artista = artista;
            this.razon = razon;
        }

        public String getTitulo() {
            return titulo;
        }

        public void setTitulo(String titulo) {
            this.titulo = titulo;
        }

        public String getArtista() {
            return artista;
        }

        public void setArtista(String artista) {
            this.artista = artista;
        }

        public String getRazon() {
            return razon;
        }

        public void setRazon(String razon) {
            this.razon = razon;
        }
    }
}
