package com.playlist.spotifyplaylistapi.service;

import com.playlist.spotifyplaylistapi.model.Playlist;
import com.playlist.spotifyplaylistapi.model.Song;
import com.playlist.spotifyplaylistapi.model.dto.RecommendationResponse;
import com.playlist.spotifyplaylistapi.model.dto.RecommendationResponse.RecommendationItem;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MockRecommendationService implements IARecommendationService {

    private static final Map<String, List<RecommendationItem>> RECOMMENDATIONS_BY_GENRE = new LinkedHashMap<>();

    static {
        RECOMMENDATIONS_BY_GENRE.put("rock", List.of(
                new RecommendationItem("Born to Run", "Bruce Springsteen", "Clásico del rock con la misma energía"),
                new RecommendationItem("Paint It Black", "The Rolling Stones", "Rock con guitarra icónica"),
                new RecommendationItem("Whole Lotta Love", "Led Zeppelin", "Riff legendario del rock duro"),
                new RecommendationItem("Paranoid", "Black Sabbath", "Precursor del metal con esencia rock"),
                new RecommendationItem("Won't Get Fooled Again", "The Who", "Himno del rock con sintetizador")
        ));
        RECOMMENDATIONS_BY_GENRE.put("pop", List.of(
                new RecommendationItem("Blinding Lights", "The Weeknd", "Pop ochentero con sintetizadores"),
                new RecommendationItem("Levitating", "Dua Lipa", "Pop disco moderno y bailable"),
                new RecommendationItem("Shape of You", "Ed Sheeran", "Pop acústico con ritmo pegajoso"),
                new RecommendationItem("Uptown Funk", "Mark Ronson ft. Bruno Mars", "Funk-pop energético"),
                new RecommendationItem("Bad Guy", "Billie Eilish", "Pop minimalista y vanguardista")
        ));
        RECOMMENDATIONS_BY_GENRE.put("jazz", List.of(
                new RecommendationItem("My Funny Valentine", "Chet Baker", "Standard de jazz con trompeta melódica"),
                new RecommendationItem("Giant Steps", "John Coltrane", "Jazz virtuoso con cambios armónicos"),
                new RecommendationItem("Round Midnight", "Thelonious Monk", "Jazz atmosférico y nocturno"),
                new RecommendationItem("Feeling Good", "Nina Simone", "Voz soul con arreglos de jazz"),
                new RecommendationItem("Spain", "Chick Corea", "Jazz fusión con influencias latinas")
        ));
        RECOMMENDATIONS_BY_GENRE.put("blues", List.of(
                new RecommendationItem("The Thrill Is Gone", "B.B. King", "Blues eléctrico con Lucille"),
                new RecommendationItem("Red House", "Jimi Hendrix", "Blues psicodélico con guitarra"),
                new RecommendationItem("Damn Right I've Got the Blues", "Buddy Guy", "Blues contemporáneo potente"),
                new RecommendationItem("Pride and Joy", "Stevie Ray Vaughan", "Blues texano con técnica impecable")
        ));
        RECOMMENDATIONS_BY_GENRE.put("metal", List.of(
                new RecommendationItem("Master of Puppets", "Metallica", "Thrash metal épico y atemporal"),
                new RecommendationItem("The Number of the Beast", "Iron Maiden", "Metal clásico con voz operática"),
                new RecommendationItem("Raining Blood", "Slayer", "Metal extremo y veloz"),
                new RecommendationItem("Holy Diver", "Dio", "Metal con voz poderosa y riffs")
        ));
        RECOMMENDATIONS_BY_GENRE.put("electronic", List.of(
                new RecommendationItem("Strobe", "deadmau5", "Electrónica progresiva hipnótica"),
                new RecommendationItem("Midnight City", "M83", "Electrónica atmosférica con saxofón"),
                new RecommendationItem("One More Time", "Daft Punk", "House francés con vocoder"),
                new RecommendationItem("Windowlicker", "Aphex Twin", "IDM experimental y vanguardista")
        ));
        RECOMMENDATIONS_BY_GENRE.put("hip-hop", List.of(
                new RecommendationItem("Juicy", "The Notorious B.I.G.", "Hip-hop narrativo del este"),
                new RecommendationItem("N.Y. State of Mind", "Nas", "Lírica callejera y producción boom bap"),
                new RecommendationItem("Alright", "Kendrick Lamar", "Hip-hop consciente con jazz"),
                new RecommendationItem("Lose Yourself", "Eminem", "Rap motivacional con entrega intensa")
        ));
        RECOMMENDATIONS_BY_GENRE.put("classical", List.of(
                new RecommendationItem("Clair de Lune", "Debussy", "Impresionismo pianístico etéreo"),
                new RecommendationItem("Eine Kleine Nachtmusik", "Mozart", "Clasicismo vienés alegre"),
                new RecommendationItem("Ride of the Valkyries", "Wagner", "Ópera épica y dramática"),
                new RecommendationItem("Adagio for Strings", "Barber", "Música orquestal emotiva")
        ));
    }

    private static final List<RecommendationItem> DEFAULT_RECOMMENDATIONS = List.of(
            new RecommendationItem("Bohemian Rhapsody", "Queen", "Obra maestra del rock que combina géneros"),
            new RecommendationItem("Hotel California", "Eagles", "Rock clásico con solo de guitarra inolvidable"),
            new RecommendationItem("Imagine", "John Lennon", "Balada atemporal de rock/pop"),
            new RecommendationItem("Like a Rolling Stone", "Bob Dylan", "Rock poético que cambió la música"),
            new RecommendationItem("Smells Like Teen Spirit", "Nirvana", "Grunge que definió una generación")
    );

    @Override
    public RecommendationResponse recommend(Playlist playlist) {
        List<Song> songs = playlist.getCanciones();
        if (songs == null || songs.isEmpty()) {
            return new RecommendationResponse(playlist.getNombre(), DEFAULT_RECOMMENDATIONS);
        }

        Set<String> existing = songs.stream()
                .map(s -> s.getTitulo().toLowerCase().trim())
                .collect(Collectors.toSet());

        Set<String> genres = songs.stream()
                .map(Song::getGenero)
                .filter(g -> g != null && !g.isBlank())
                .map(g -> g.split(",")[0].trim().toLowerCase())
                .collect(Collectors.toSet());

        List<RecommendationItem> items = new ArrayList<>();

        for (String genre : genres) {
            List<RecommendationItem> genreRecs = findGenreRecs(genre);
            for (RecommendationItem item : genreRecs) {
                if (!existing.contains(item.getTitulo().toLowerCase()) && items.size() < 5) {
                    items.add(item);
                }
            }
            if (items.size() >= 5) break;
        }

        for (RecommendationItem defaultRec : DEFAULT_RECOMMENDATIONS) {
            if (items.size() >= 5) break;
            if (!existing.contains(defaultRec.getTitulo().toLowerCase())) {
                items.add(defaultRec);
            }
        }

        return new RecommendationResponse(playlist.getNombre(), items);
    }

    private List<RecommendationItem> findGenreRecs(String genre) {
        for (Map.Entry<String, List<RecommendationItem>> entry : RECOMMENDATIONS_BY_GENRE.entrySet()) {
            if (entry.getKey().contains(genre) || genre.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return DEFAULT_RECOMMENDATIONS;
    }
}
