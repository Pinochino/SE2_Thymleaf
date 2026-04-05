package com.example.SE2.services.scraper;

import com.example.SE2.models.Chapter;
import com.example.SE2.models.Novel;
import com.example.SE2.repositories.ChapterRepository;
import com.example.SE2.repositories.NovelRepository;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChapterScraperService {

    private static final Logger log = LoggerFactory.getLogger(ChapterScraperService.class);

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Gutenberg book ID -> novel title substring to match in DB.
     * Curated list of public domain novels across genres.
     */
    private static final LinkedHashMap<Integer, String> GUTENBERG_BOOKS = new LinkedHashMap<>() {{
        // HORROR
        put(84, "Frankenstein");
        put(345, "Dracula");
        put(174, "Dorian Gray");
        put(696, "Jekyll");  // Strange Case of Dr Jekyll and Mr Hyde

        // CRIME
        put(1661, "Sherlock Holmes");  // Adventures of Sherlock Holmes
        put(730, "Oliver Twist");
        put(2852, "Hound of the Baskervilles");

        // ROMANCE
        put(1342, "Pride and Prejudice");
        put(161, "Sense and Sensibility");
        put(768, "Wuthering Heights");
        put(1260, "Jane Eyre");

        // FANTASY
        put(11, "Alice");  // Alice's Adventures in Wonderland
        put(35, "Time Machine");

        // SCIFI
        put(36, "War of the Worlds");
        put(164, "Twenty Thousand Leagues");

        // HISTORIC
        put(98, "Tale of Two Cities");
        put(1399, "Anna Karenina");

        // COMEDY
        put(76, "Huckleberry Finn");
        put(1400, "Great Expectations");
    }};

    /**
     * Scrape chapters from Project Gutenberg for novels already in DB.
     * @param maxBooks max number of books to process
     * @return total chapters saved
     */
    public int scrapeChapters(int maxBooks) {
        int totalChapters = 0;
        int booksProcessed = 0;

        for (Map.Entry<Integer, String> entry : GUTENBERG_BOOKS.entrySet()) {
            if (booksProcessed >= maxBooks) break;

            int gutenbergId = entry.getKey();
            String titleMatch = entry.getValue();

            // Find matching novel in DB
            Novel novel = findNovelByTitleContaining(titleMatch);
            if (novel == null) {
                log.warn("No novel found matching '{}', skipping Gutenberg #{}", titleMatch, gutenbergId);
                continue;
            }

            // Skip if novel already has chapters
            List<Chapter> existingChapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novel.getId());
            if (!existingChapters.isEmpty()) {
                log.info("Novel '{}' already has {} chapters, skipping", novel.getTitle(), existingChapters.size());
                booksProcessed++;
                continue;
            }

            log.info("Fetching Gutenberg #{} for novel '{}'...", gutenbergId, novel.getTitle());
            String fullText = fetchGutenbergText(gutenbergId);
            if (fullText == null) {
                log.warn("Failed to fetch text for Gutenberg #{}", gutenbergId);
                continue;
            }

            List<ParsedChapter> parsed = parseChapters(fullText);
            if (parsed.isEmpty()) {
                log.warn("No chapters parsed from Gutenberg #{}", gutenbergId);
                continue;
            }

            // Limit to max 30 chapters per book
            int limit = Math.min(parsed.size(), 30);
            for (int i = 0; i < limit; i++) {
                ParsedChapter pc = parsed.get(i);
                Chapter chapter = new Chapter();
                chapter.setNovel(novel);
                chapter.setChapterNumber((long) (i + 1));
                chapter.setTitle(pc.title);
                chapter.setContent(pc.content);
                chapter.setParagraphs(pc.paragraphCount);
                chapterRepository.save(chapter);
                totalChapters++;
            }

            log.info("Saved {} chapters for '{}'", limit, novel.getTitle());
            booksProcessed++;

            // Rate limit
            sleep(2000);
        }

        log.info("Chapter scraping complete. {} chapters saved across {} books", totalChapters, booksProcessed);
        return totalChapters;
    }

    private Novel findNovelByTitleContaining(String titleMatch) {
        // Search DB for a novel whose title contains the match string
        List<Novel> all = novelRepository.findAll();
        return all.stream()
                .filter(n -> n.getTitle().toLowerCase().contains(titleMatch.toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    private String fetchGutenbergText(int gutenbergId) {
        // Try UTF-8 text first, then fallback
        String[] urls = {
                "https://www.gutenberg.org/cache/epub/" + gutenbergId + "/pg" + gutenbergId + ".txt",
                "https://www.gutenberg.org/files/" + gutenbergId + "/" + gutenbergId + "-0.txt"
        };

        for (String url : urls) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "SE2-NovelApp/1.0 (educational project)")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    return stripGutenbergBoilerplate(response.body());
                }
            } catch (Exception e) {
                log.debug("Failed to fetch {}: {}", url, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Remove Project Gutenberg header/footer boilerplate.
     */
    private String stripGutenbergBoilerplate(String text) {
        // Find start marker
        int start = 0;
        String[] startMarkers = {"*** START OF THE PROJECT GUTENBERG", "*** START OF THIS PROJECT GUTENBERG",
                "***START OF THE PROJECT GUTENBERG"};
        for (String marker : startMarkers) {
            int idx = text.indexOf(marker);
            if (idx >= 0) {
                start = text.indexOf('\n', idx) + 1;
                break;
            }
        }

        // Find end marker
        int end = text.length();
        String[] endMarkers = {"*** END OF THE PROJECT GUTENBERG", "*** END OF THIS PROJECT GUTENBERG",
                "***END OF THE PROJECT GUTENBERG", "End of the Project Gutenberg", "End of Project Gutenberg"};
        for (String marker : endMarkers) {
            int idx = text.indexOf(marker);
            if (idx >= 0) {
                end = idx;
                break;
            }
        }

        return text.substring(start, end).trim();
    }

    /**
     * Parse full text into chapters. Handles various chapter heading formats:
     * "CHAPTER I", "Chapter 1", "CHAPTER ONE", "I.", "BOOK ONE", etc.
     */
    private List<ParsedChapter> parseChapters(String text) {
        List<ParsedChapter> chapters = new ArrayList<>();

        // Pattern matches common chapter heading formats
        Pattern chapterPattern = Pattern.compile(
                "(?m)^\\s*(?:CHAPTER|Chapter)\\s+([IVXLCDM]+|\\d+|[A-Z][A-Z]+)(?:[.:\\s—–-].*)?$",
                Pattern.MULTILINE
        );

        Matcher matcher = chapterPattern.matcher(text);
        List<int[]> chapterPositions = new ArrayList<>(); // [start_of_heading, start_of_content]
        List<String> chapterTitles = new ArrayList<>();

        while (matcher.find()) {
            chapterPositions.add(new int[]{matcher.start(), matcher.end()});
            String heading = matcher.group().trim();
            // Extract a clean title
            String title = cleanChapterTitle(heading);
            chapterTitles.add(title);
        }

        if (chapterPositions.isEmpty()) {
            // Fallback: split by large gaps or numbered sections
            return splitByParagraphBlocks(text);
        }

        // Extract content between chapter headings
        for (int i = 0; i < chapterPositions.size(); i++) {
            int contentStart = chapterPositions.get(i)[1];
            int contentEnd = (i + 1 < chapterPositions.size())
                    ? chapterPositions.get(i + 1)[0]
                    : text.length();

            String rawContent = text.substring(contentStart, contentEnd).trim();
            String content = normalizeParagraphs(rawContent);

            if (content.length() < 200) continue; // skip tiny chapters

            int paragraphCount = content.split("\n\n+").length;
            chapters.add(new ParsedChapter(chapterTitles.get(i), content, paragraphCount));
        }

        return chapters;
    }

    /**
     * Fallback: if no CHAPTER headings found, split text into ~2000-word blocks.
     */
    private List<ParsedChapter> splitByParagraphBlocks(String text) {
        List<ParsedChapter> chapters = new ArrayList<>();
        String normalized = normalizeParagraphs(text);
        String[] paragraphs = normalized.split("\n\n+");

        List<String> currentBlock = new ArrayList<>();
        int wordCount = 0;
        int chapterNum = 1;

        for (String para : paragraphs) {
            String trimmed = para.trim();
            if (trimmed.isEmpty()) continue;

            currentBlock.add(trimmed);
            wordCount += trimmed.split("\\s+").length;

            if (wordCount >= 2000) {
                String content = String.join("\n\n", currentBlock);
                int pCount = currentBlock.size();
                chapters.add(new ParsedChapter("Chapter " + chapterNum, content, pCount));
                currentBlock.clear();
                wordCount = 0;
                chapterNum++;
            }
        }

        // Remaining paragraphs
        if (!currentBlock.isEmpty()) {
            String content = String.join("\n\n", currentBlock);
            chapters.add(new ParsedChapter("Chapter " + chapterNum, content, currentBlock.size()));
        }

        return chapters;
    }

    /**
     * Normalize text into proper paragraphs separated by \n\n.
     * Gutenberg text uses single newlines for line wrapping within paragraphs.
     */
    private String normalizeParagraphs(String raw) {
        // Replace Windows line endings
        String text = raw.replace("\r\n", "\n");

        // Split on double+ newlines (actual paragraph breaks)
        String[] rawParagraphs = text.split("\n\\s*\n");

        List<String> paragraphs = new ArrayList<>();
        for (String p : rawParagraphs) {
            // Join single-newline-wrapped lines within a paragraph
            String joined = p.trim().replaceAll("\\s*\n\\s*", " ").trim();
            if (joined.isEmpty()) continue;

            // Skip very short lines that are likely headers/footers
            if (joined.length() < 10 && !joined.contains(".")) continue;

            paragraphs.add(joined);
        }

        return String.join("\n\n", paragraphs);
    }

    private String cleanChapterTitle(String heading) {
        // Remove "CHAPTER" prefix and clean up
        String title = heading.replaceAll("(?i)^\\s*chapter\\s+", "").trim();
        // Remove trailing punctuation
        title = title.replaceAll("[.:\\s—–-]+$", "").trim();
        if (title.isEmpty()) return "Untitled";
        return "Chapter " + title;
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // Simple data holder for parsed chapter
    private static class ParsedChapter {
        final String title;
        final String content;
        final int paragraphCount;

        ParsedChapter(String title, String content, int paragraphCount) {
            this.title = title;
            this.content = content;
            this.paragraphCount = paragraphCount;
        }
    }
}
