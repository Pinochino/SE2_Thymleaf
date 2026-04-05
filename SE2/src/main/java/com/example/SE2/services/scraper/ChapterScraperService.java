package com.example.SE2.services.scraper;

import com.example.SE2.constants.GenreName;
import com.example.SE2.constants.NovelStatus;
import com.example.SE2.models.Chapter;
import com.example.SE2.models.Genre;
import com.example.SE2.models.Novel;
import com.example.SE2.repositories.ChapterRepository;
import com.example.SE2.repositories.GenreRepository;
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

    @Autowired
    private GenreRepository genreRepository;

    /** Gutenberg book metadata: id, title, author, genre, search key */
    private record BookInfo(int id, String title, String author, GenreName genre, String searchKey) {}

    private static final List<BookInfo> GUTENBERG_BOOKS = List.of(
        // HORROR
        new BookInfo(84, "Frankenstein", "Mary Shelley", GenreName.HORROR, "Frankenstein"),
        new BookInfo(345, "Dracula", "Bram Stoker", GenreName.HORROR, "Dracula"),
        new BookInfo(174, "The Picture of Dorian Gray", "Oscar Wilde", GenreName.HORROR, "Dorian Gray"),
        new BookInfo(696, "Strange Case of Dr Jekyll and Mr Hyde", "Robert Louis Stevenson", GenreName.HORROR, "Jekyll"),
        // CRIME
        new BookInfo(1661, "The Adventures of Sherlock Holmes", "Arthur Conan Doyle", GenreName.CRIME, "Sherlock Holmes"),
        new BookInfo(730, "Oliver Twist", "Charles Dickens", GenreName.CRIME, "Oliver Twist"),
        new BookInfo(2852, "The Hound of the Baskervilles", "Arthur Conan Doyle", GenreName.CRIME, "Hound of the Baskerville"),
        // ROMANCE
        new BookInfo(1342, "Pride and Prejudice", "Jane Austen", GenreName.ROMANCE, "Pride and Prejudice"),
        new BookInfo(161, "Sense and Sensibility", "Jane Austen", GenreName.ROMANCE, "Sense and Sensibility"),
        new BookInfo(768, "Wuthering Heights", "Emily Bronte", GenreName.ROMANCE, "Wuthering Heights"),
        new BookInfo(1260, "Jane Eyre", "Charlotte Bronte", GenreName.ROMANCE, "Jane Eyre"),
        // FANTASY
        new BookInfo(11, "Alice's Adventures in Wonderland", "Lewis Carroll", GenreName.FANTASY, "Alice"),
        new BookInfo(35, "The Time Machine", "H.G. Wells", GenreName.FANTASY, "Time Machine"),
        // SCIFI
        new BookInfo(36, "The War of the Worlds", "H.G. Wells", GenreName.SCIFI, "War of the Worlds"),
        new BookInfo(164, "Twenty Thousand Leagues Under the Sea", "Jules Verne", GenreName.SCIFI, "Twenty Thousand"),
        // HISTORIC
        new BookInfo(98, "A Tale of Two Cities", "Charles Dickens", GenreName.HISTORIC, "Tale of Two Cities"),
        new BookInfo(1399, "Anna Karenina", "Leo Tolstoy", GenreName.HISTORIC, "Anna Karenina"),
        // COMEDY
        new BookInfo(76, "Adventures of Huckleberry Finn", "Mark Twain", GenreName.COMEDY, "Huckleberry Finn"),
        new BookInfo(1400, "Great Expectations", "Charles Dickens", GenreName.COMEDY, "Great Expectations")
    );

    /**
     * Scrape chapters from Project Gutenberg for novels already in DB.
     * @param maxBooks max number of books to process
     * @return total chapters saved
     */
    public int scrapeChapters(int maxBooks) {
        int totalChapters = 0;
        int booksProcessed = 0;

        for (BookInfo book : GUTENBERG_BOOKS) {
            if (booksProcessed >= maxBooks) break;

            // Find or create novel in DB
            Novel novel = findNovelByTitleContaining(book.searchKey());
            if (novel == null) {
                novel = createNovel(book);
                log.info("Created novel '{}' in DB", novel.getTitle());
            }

            // Skip if novel already has chapters
            List<Chapter> existingChapters = chapterRepository.findByNovelIdOrderByChapterNumberAsc(novel.getId());
            if (!existingChapters.isEmpty()) {
                log.info("Novel '{}' already has {} chapters, skipping", novel.getTitle(), existingChapters.size());
                booksProcessed++;
                continue;
            }

            log.info("Fetching Gutenberg #{} for novel '{}'...", book.id(), novel.getTitle());
            String fullText = fetchGutenbergText(book.id());
            if (fullText == null) {
                log.warn("Failed to fetch text for Gutenberg #{}", book.id());
                continue;
            }

            List<ParsedChapter> parsed = parseChapters(fullText);
            if (parsed.isEmpty()) {
                log.warn("No chapters parsed from Gutenberg #{}", book.id());
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

    private Novel createNovel(BookInfo book) {
        Novel novel = new Novel();
        novel.setPublicId(UUID.randomUUID());
        novel.setTitle(book.title());
        novel.setAuthor(book.author());
        novel.setDescription("A classic novel by " + book.author() + ".");
        novel.setStatus(NovelStatus.COMPLETED);
        novel.setAverageRating(4.0f + new Random().nextFloat());
        novel.setCoverImgUrl("https://covers.openlibrary.org/b/id/" + (book.id() * 10) + "-L.jpg");
        novel = novelRepository.save(novel);

        // Link genre
        Genre genre = genreRepository.findGenreByName(book.genre());
        if (genre == null) {
            genre = genreRepository.save(new Genre(book.genre()));
        }
        genre.getNovels().add(novel);
        genreRepository.save(genre);

        return novel;
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
