package com.example.SE2.configs;

import com.example.SE2.service.NovelPersistenceService;
import com.example.SE2.service.NovelPersistenceService.PersistResult;
import com.example.SE2.service.NovelUpdateService;
import com.example.SE2.service.NovelUpdateService.NovelWithContent;
import com.example.SE2.service.NovelUpdateService.TimePeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NovelImportRunner - Cào 20 truyện từ các site và import vào DB.
 *
 * Chỉ chạy khi active profile "import-novels":
 *   ./mvnw spring-boot:run -Dspring-boot.run.profiles=import-novels
 *
 * Hoặc trong IntelliJ: Edit Configurations → Active profiles: import-novels
 *
 * Flow:
 *   1. Cào 20 truyện mới cập nhật (mỗi truyện lấy 2 chapter mới nhất)
 *   2. Lưu Novel, Chapter, Genre vào DB (dedup theo title + chapterNumber)
 *   3. In kết quả tổng kết
 */
@Component
@Profile("import-novels")
public class NovelImportRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(NovelImportRunner.class);

    private final NovelPersistenceService persistenceService;

    /** Danh sách site sẽ cào, thử lần lượt đến khi thành công */
    private static final String[] SITES = {"royalroad", "truyenfull", "metruyencv", "biquge", "69shu"};

    /** Số truyện cào */
    private static final int NOVEL_COUNT = 20;

    /** Số chapter mới nhất lấy content cho mỗi truyện */
    private static final int CHAPTERS_PER_NOVEL = 2;

    public NovelImportRunner(NovelPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("══════════════════════════════════════════════════════");
        log.info("  NOVEL IMPORT RUNNER - Bắt đầu cào & import vào DB");
        log.info("══════════════════════════════════════════════════════");

        NovelUpdateService updateService = new NovelUpdateService();

        for (String site : SITES) {
            log.info("Đang thử site: {}", site);
            try {
                // 1. Cào dữ liệu
                List<NovelWithContent> novels = updateService.scrapeUpdatesWithContent(
                        site, TimePeriod.LAST_MONTH, NOVEL_COUNT, CHAPTERS_PER_NOVEL);

                if (novels.isEmpty()) {
                    log.warn("Site {} không trả về dữ liệu, thử site tiếp theo...", site);
                    continue;
                }

                log.info("Đã cào {} truyện từ {}, bắt đầu lưu vào DB...", novels.size(), site);

                // 2. Lưu vào DB
                List<PersistResult> results = persistenceService.saveAll(novels);

                // 3. Tổng kết
                int newCount = (int) results.stream().filter(PersistResult::isNew).count();
                int updatedCount = results.size() - newCount;
                int totalChapters = results.stream().mapToInt(PersistResult::chaptersSaved).sum();

                log.info("══════════════════════════════════════════════════════");
                log.info("  HOÀN TẤT IMPORT TỪ: {}", site);
                log.info("  Tổng truyện: {} ({} mới, {} cập nhật)", results.size(), newCount, updatedCount);
                log.info("  Tổng chapter: {}", totalChapters);
                log.info("══════════════════════════════════════════════════════");

                for (PersistResult r : results) {
                    log.info("  {} [{}] - {} chapter | {}",
                            r.isNew() ? "✓ NEW" : "↻ UPD",
                            r.novel().getId(),
                            r.chaptersSaved(),
                            r.novel().getTitle());
                }

                // Đã import thành công → dừng
                break;

            } catch (Exception e) {
                log.error("Lỗi cào site {}: {}", site, e.getMessage());
            }
        }

        log.info("Novel Import Runner kết thúc.");
    }
}
