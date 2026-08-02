package com.playlist.spotifyplaylistapi.service;

import com.playlist.spotifyplaylistapi.model.Playlist;
import com.playlist.spotifyplaylistapi.model.dto.RecommendationResponse;

public interface IARecommendationService {

    RecommendationResponse recommend(Playlist playlist);
}
