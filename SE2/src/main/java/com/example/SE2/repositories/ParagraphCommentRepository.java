package com.example.SE2.repositories;

import com.example.SE2.models.ParagraphComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParagraphCommentRepository extends JpaRepository<ParagraphComment, Long> {
    List<ParagraphComment> findByChapterIdAndParentCommentIsNullOrderByCreatedAtDesc(Long chapterId);

    List<ParagraphComment> findByChapterIdAndParagraphIndexAndParentCommentIsNullOrderByCreatedAtDesc(Long chapterId, Integer paragraphIndex);

    Long countByChapterIdAndParagraphIndex(Long chapterId, Integer paragraphIndex);
}
