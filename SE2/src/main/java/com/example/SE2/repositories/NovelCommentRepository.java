package com.example.SE2.repositories;

import com.example.SE2.models.NovelComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NovelCommentRepository extends JpaRepository<NovelComment, Long> {
}
