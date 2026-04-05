package com.example.SE2.services.scraper;

import com.example.SE2.constants.GenreName;
import com.example.SE2.constants.NovelStatus;
import com.example.SE2.models.Genre;
import com.example.SE2.models.Novel;
import com.example.SE2.repositories.GenreRepository;
import com.example.SE2.repositories.NovelRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class NovelScraperService {

    private static final Logger log = LoggerFactory.getLogger(NovelScraperService.class);
    private static final String OL_SEARCH = "https://openlibrary.org/search.json";

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private GenreRepository genreRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    // Genre search queries mapped to our GenreName enum
    private static final Map<GenreName, String[]> GENRE_QUERIES = Map.of(
            GenreName.FANTASY, new String[]{"fantasy novel", "epic fantasy fiction"},
            GenreName.HORROR, new String[]{"horror novel", "gothic horror fiction"},
            GenreName.ROMANCE, new String[]{"romance novel", "love story fiction"},
            GenreName.HISTORIC, new String[]{"historical fiction novel", "historical novel"},
            GenreName.SCIFI, new String[]{"science fiction novel", "cyberpunk dystopia"},
            GenreName.COMEDY, new String[]{"comedy novel", "humorous fiction satire"},
            GenreName.CRIME, new String[]{"crime fiction novel", "mystery thriller detective"}
    );

    /**
     * Scrape novels from Open Library API and save to DB.
     * @param totalTarget target number of novels to scrape (across all genres)
     * @return number of novels actually saved
     */
    public int scrapeAndSave(int totalTarget) {
        int perGenre = Math.max(1, totalTarget / GENRE_QUERIES.size());
        int saved = 0;

        // Ensure all genres exist in DB
        Map<GenreName, Genre> genreMap = ensureGenresExist();

        for (Map.Entry<GenreName, String[]> entry : GENRE_QUERIES.entrySet()) {
            GenreName genreName = entry.getKey();
            Genre genre = genreMap.get(genreName);

            for (String query : entry.getValue()) {
                if (saved >= totalTarget) break;

                int needed = Math.min(perGenre, totalTarget - saved);
                int fetched = fetchAndSaveFromOpenLibrary(query, needed, genre, genreName);
                saved += fetched;
                log.info("Genre [{}] query [{}]: saved {} novels (total: {})", genreName, query, fetched, saved);

                // Rate limit — Open Library asks for < 1 req/sec for anonymous
                sleep(1200);
            }
        }

        log.info("Scraping complete. Total novels saved: {}", saved);
        return saved;
    }

    private int fetchAndSaveFromOpenLibrary(String query, int limit, Genre genre, GenreName genreName) {
        int saved = 0;
        try {
            String url = OL_SEARCH
                    + "?q=" + query.replace(" ", "+")
                    + "&limit=" + (limit + 10) // fetch extra in case of dupes
                    + "&fields=title,author_name,first_sentence,cover_i,subject,ratings_average"
                    + "&language=eng";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "SE2-NovelApp/1.0 (educational project)")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Open Library returned status {} for query: {}", response.statusCode(), query);
                return 0;
            }

            JsonNode root = mapper.readTree(response.body());
            JsonNode docs = root.get("docs");
            if (docs == null || !docs.isArray()) return 0;

            for (JsonNode doc : docs) {
                if (saved >= limit) break;

                String title = getTextOrNull(doc, "title");
                if (title == null || title.length() < 3) continue;
                if (novelRepository.existsByTitle(title)) continue;

                // Build description from first_sentence
                String description = buildDescription(doc);
                String author = getFirstFromArray(doc, "author_name");
                String coverUrl = buildCoverUrl(doc);
                Float rating = getFloatOrNull(doc, "ratings_average");
                NovelStatus status = pickRandomStatus();

                // Detect additional genres from subjects
                Set<GenreName> detectedGenres = detectGenres(doc);
                detectedGenres.add(genreName); // always include the primary genre

                Novel novel = new Novel();
                novel.setPublicId(UUID.randomUUID());
                novel.setTitle(title);
                novel.setDescription(description);
                novel.setAuthor(author != null ? author : "Unknown");
                novel.setStatus(status);
                novel.setAverageRating(rating != null ? Math.round(rating * 10) / 10f : randomRating());
                novel.setCoverImgUrl(coverUrl);
                novelRepository.save(novel);

                // Link genres
                for (GenreName gn : detectedGenres) {
                    Genre g = ensureGenreExists(gn);
                    g.getNovels().add(novel);
                    genreRepository.save(g);
                }

                saved++;
            }
        } catch (Exception e) {
            log.error("Error fetching from Open Library for query [{}]: {}", query, e.getMessage());
        }
        return saved;
    }

    private Map<GenreName, Genre> ensureGenresExist() {
        Map<GenreName, Genre> map = new HashMap<>();
        for (GenreName name : GenreName.values()) {
            map.put(name, ensureGenreExists(name));
        }
        return map;
    }

    private Genre ensureGenreExists(GenreName name) {
        Genre existing = genreRepository.findGenreByName(name);
        if (existing != null) return existing;
        return genreRepository.save(new Genre(name));
    }

    private Set<GenreName> detectGenres(JsonNode doc) {
        Set<GenreName> genres = new HashSet<>();
        JsonNode subjects = doc.get("subject");
        if (subjects == null || !subjects.isArray()) return genres;

        for (JsonNode s : subjects) {
            String subj = s.asText().toLowerCase();
            if (subj.contains("fantasy") || subj.contains("magic") || subj.contains("dragon"))
                genres.add(GenreName.FANTASY);
            if (subj.contains("horror") || subj.contains("gothic") || subj.contains("vampire") || subj.contains("supernatural"))
                genres.add(GenreName.HORROR);
            if (subj.contains("romance") || subj.contains("love stor"))
                genres.add(GenreName.ROMANCE);
            if (subj.contains("histor"))
                genres.add(GenreName.HISTORIC);
            if (subj.contains("science fiction") || subj.contains("sci-fi") || subj.contains("dystop") || subj.contains("cyberpunk"))
                genres.add(GenreName.SCIFI);
            if (subj.contains("humor") || subj.contains("comedy") || subj.contains("satire") || subj.contains("funny"))
                genres.add(GenreName.COMEDY);
            if (subj.contains("crime") || subj.contains("detective") || subj.contains("mystery") || subj.contains("thriller"))
                genres.add(GenreName.CRIME);
        }
        return genres;
    }

    private String buildDescription(JsonNode doc) {
        JsonNode firstSentence = doc.get("first_sentence");
        if (firstSentence != null && firstSentence.isArray() && !firstSentence.isEmpty()) {
            return firstSentence.get(0).asText();
        }
        // Fallback: build from subjects
        JsonNode subjects = doc.get("subject");
        if (subjects != null && subjects.isArray()) {
            List<String> tags = new ArrayList<>();
            for (int i = 0; i < Math.min(5, subjects.size()); i++) {
                tags.add(subjects.get(i).asText());
            }
            return "A novel exploring themes of " + String.join(", ", tags) + ".";
        }
        return "An intriguing novel waiting to be discovered.";
    }

    private String buildCoverUrl(JsonNode doc) {
        JsonNode coverId = doc.get("cover_i");
        if (coverId != null && !coverId.isNull()) {
            return "https://covers.openlibrary.org/b/id/" + coverId.asInt() + "-L.jpg";
        }
        return null;
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return (val != null && !val.isNull()) ? val.asText() : null;
    }

    private String getFirstFromArray(JsonNode node, String field) {
        JsonNode arr = node.get(field);
        if (arr != null && arr.isArray() && !arr.isEmpty()) {
            return arr.get(0).asText();
        }
        return null;
    }

    private Float getFloatOrNull(JsonNode node, String field) {
        JsonNode val = node.get(field);
        return (val != null && !val.isNull()) ? (float) val.asDouble() : null;
    }

    private NovelStatus pickRandomStatus() {
        NovelStatus[] statuses = {NovelStatus.ONGOING, NovelStatus.COMPLETED, NovelStatus.COMPLETED, NovelStatus.ONGOING};
        return statuses[new Random().nextInt(statuses.length)];
    }

    private float randomRating() {
        return Math.round((3.5f + new Random().nextFloat() * 1.5f) * 10) / 10f;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
