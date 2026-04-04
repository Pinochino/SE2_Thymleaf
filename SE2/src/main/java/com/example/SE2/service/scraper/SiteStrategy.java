package com.example.SE2.service.scraper;

import com.example.SE2.service.NovelScraperService.*;

import java.io.IOException;
import java.util.List;

/**
 * Interface cho mỗi nguồn truyện (site).
 * Mỗi site cài đặt cách lấy thông tin truyện, nội dung chương, và danh sách cập nhật.
 */
public interface SiteStrategy {

    /** Kiểm tra URL có thuộc site này không */
    boolean supports(String url);

    /** Base URL của site */
    String baseUrl();

    /** Lấy thông tin truyện + danh sách chương */
    NovelInfo fetchNovelInfo(String novelUrl) throws IOException;

    /** Lấy nội dung 1 chương */
    ChapterContent fetchChapter(ChapterLink link) throws IOException;

    /** Lấy danh sách truyện mới cập nhật (phân trang) */
    List<NovelUpdate> fetchLatestUpdates(int page) throws IOException;
}
