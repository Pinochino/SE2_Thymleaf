package com.example.SE2.repositories;

import com.example.SE2.constants.NovelStatus;
import com.example.SE2.models.Novel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {
	Novel findBookByTitle(String title);

	Novel findByTitle(String title);

	boolean existsByTitle(String title);
	Novel findNovelByPublicId(UUID publicId);

	@Query("SELECT n FROM Novel n ORDER BY n.averageRating DESC NULLS LAST")
	Page<Novel> findTrendingNovels(Pageable pageable);

	@Query("SELECT n FROM Novel n ORDER BY n.updatedAt DESC")
	Page<Novel> findRecentNovels(Pageable pageable);

	@Query("SELECT n FROM Novel n JOIN ReadingProgress rp ON rp.chapter.novel = n WHERE rp.user.id = :userId GROUP BY n ORDER BY MAX(rp.updatedAt) DESC")
	List<Novel> findCurrentlyReadingByUserId(@Param("userId") String userId, Pageable pageable);

	@Query("SELECT f.novel FROM Favorite f WHERE f.user.id = :userId")
	List<Novel> findFavoritesByUserId(String userId);

	@Query(value = """
		SELECT * FROM novel
		ORDER BY meta_vector <=> CAST(:queryVector AS vector)
		LIMIT :limit OFFSET :offset
		""", nativeQuery = true)
	List<Novel> searchVector(
			@Param("queryVector") String queryVector,
			@Param("limit")       int limit,
			@Param("offset")      int offset
	);

	@Query(value = "SELECT COUNT(*) FROM novel", nativeQuery = true)
	long countAllNovels();

	@Query(value = """
	SELECT DISTINCT n FROM Novel n
	LEFT JOIN n.genres g
	WHERE
		(:trending IS NULL OR (:trending = true AND n.averageRating > 4.0))
		AND (:#{#genres == null || #genres.isEmpty()} = true OR g.id IN :genres)
		AND (:status IS NULL OR n.status = :status)
	""",
			countQuery = """
	SELECT COUNT(DISTINCT n) FROM Novel n
	LEFT JOIN n.genres g
	WHERE
		(:trending IS NULL OR (:trending = true AND n.averageRating > 4.0))
		AND (:#{#genres == null || #genres.isEmpty()} = true OR g.id IN :genres)
		AND (:status IS NULL OR n.status = :status)
	"""
	)
	Page<Novel> searchFilter(
			@Param("trending") Boolean trending,
			@Param("genres") List<String> genres,
			@Param("status") NovelStatus status,
			Pageable pageable
	);

    @Query("SELECT n FROM Novel n ORDER BY n.updatedAt DESC")
    Page<Novel> findAllNovels(Pageable pageable);

    @Query(value = """
        SELECT * FROM novel
        WHERE to_tsvector('english', coalesce(title,'') || ' ' || coalesce(author,'') || ' ' || coalesce(description,''))
              @@ plainto_tsquery('english', :query)
        ORDER BY ts_rank(
            to_tsvector('english', coalesce(title,'') || ' ' || coalesce(author,'') || ' ' || coalesce(description,'')),
            plainto_tsquery('english', :query)
        ) DESC
    """,
    countQuery = """
        SELECT count(*) FROM novel
        WHERE to_tsvector('english', coalesce(title,'') || ' ' || coalesce(author,'') || ' ' || coalesce(description,''))
              @@ plainto_tsquery('english', :query)
    """,
    nativeQuery = true)
    Page<Novel> searchFullText(@Param("query") String query, Pageable pageable);

    @Query("""
        SELECT DISTINCT n FROM Novel n
        JOIN n.genres g
        WHERE g.id IN :genreIds AND n.id NOT IN :excludeIds
        ORDER BY n.updatedAt DESC
    """)
    List<Novel> findByGenreIdsExcluding(
            @Param("genreIds") List<Long> genreIds,
            @Param("excludeIds") List<Long> excludeIds,
            Pageable pageable
    );

}
