package com.example.SE2.repositories;

import com.example.SE2.models.ParagraphComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParagraphCommentRepository extends JpaRepository<ParagraphComment, Long> {
}
