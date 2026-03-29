package com.example.SE2.repositories;

import com.example.SE2.models.Novel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {
    Novel findBookByTitle(String title);

    Novel findNovelByPublicId(UUID publicId);
}
