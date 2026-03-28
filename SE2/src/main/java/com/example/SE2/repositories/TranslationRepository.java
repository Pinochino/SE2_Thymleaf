package com.example.SE2.repositories;

import com.example.SE2.models.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {

//    @Query("""
//            SELECT *
//            FROM translations
//            WHERE asissigned_by = :userId
//            """)
//    public List<Translation> findTranslationsContributedByUser(String userId);

    public List<Translation> findByAssignedBy_Id(String userId);
}
