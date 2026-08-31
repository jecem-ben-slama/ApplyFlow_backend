package com.applyflow.tracker_api.repositories;

import com.applyflow.tracker_api.dtos.ApplicationBucketDto;
import com.applyflow.tracker_api.dtos.ApplicationSummaryDto;
import com.applyflow.tracker_api.models.Application;
import com.applyflow.tracker_api.models.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Eagerly join skills collection on base lookup
    @EntityGraph(attributePaths = { "skills" })
    Page<Application> findByUserId(Long userId, Pageable pageable);

    Optional<Application> findByIdAndUserId(Long id, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Application a SET a.user = :realUser WHERE a.user.id = :guestId")
    void reassignOwner(@Param("guestId") Long guestId, @Param("realUser") User realUser);

    // Eagerly join skills collection when filtering by status only
    @EntityGraph(attributePaths = { "skills" })
    Page<Application> findByUserIdAndStatusIgnoreCase(Long userId, String status, Pageable pageable);

    // Single dynamic query for user-facing list/search — status, keyword, and
    // language are all optional (pass null to skip). Uses CAST(:x AS string) IS
    // NULL, matching the pattern used elsewhere in this repo, since Hibernate
    // needs the cast to resolve the parameter type when the value may be null.
    //
    // IMPORTANT: every occurrence of a nullable String param that's passed into
    // a function (LOWER, LIKE, CONCAT) must ALSO be explicitly cast, not just
    // the null-check occurrence. Postgres's JDBC driver can only infer a bind
    // parameter's type from context; a bare `:param` inside LOWER(...) with no
    // other type hint gets sent as `bytea` by default, which blows up with
    // "function lower(bytea) does not exist". Casting only the null-check copy
    // (CAST(:status AS string) IS NULL OR ...) does NOT fix the second,
    // uncast occurrence used inside LOWER(:status) — that one needs its own
    // CAST(:status AS string) too.
    @EntityGraph(attributePaths = { "skills" })
    @Query("""
            SELECT a FROM Application a
            WHERE a.user.id = :userId
            AND (CAST(:status AS string) IS NULL OR LOWER(a.status) = LOWER(CAST(:status AS string)))
            AND (CAST(:language AS string) IS NULL OR LOWER(a.language) = LOWER(CAST(:language AS string)))
            AND (CAST(:keyword AS string) IS NULL OR
                 LOWER(a.companyName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                 LOWER(a.jobTitle) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<Application> findByUserIdWithFilters(@Param("userId") Long userId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("language") String language,
            Pageable pageable);

    // Eagerly join skills collection when searching by keyword
    @EntityGraph(attributePaths = { "skills" })
    @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND " +
            "(LOWER(a.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Application> searchByKeyword(@Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable);

    // Eagerly join skills collection when filtering by keyword AND status combined
    @EntityGraph(attributePaths = { "skills" })
    @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND " +
            "LOWER(a.status) = LOWER(:status) AND " +
            "(LOWER(a.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Application> searchByKeywordAndStatus(@Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable);

    // Total application count for a user, for the stats summary (all-time),
    // excluding 'compiled'
    @Query("SELECT COUNT(a) FROM Application a WHERE a.user.id = :userId AND (a.status IS NULL OR LOWER(a.status) <> 'compiled')")
    long countByUserId(@Param("userId") Long userId);

    // Date-range aware total, keyed off dateApplied, excluding 'compiled'
    @Query("""
            SELECT COUNT(a) FROM Application a
            WHERE a.user.id = :userId
            AND a.dateApplied >= :from
            AND a.dateApplied <= :to
            AND (a.status IS NULL OR LOWER(a.status) <> 'compiled')
            """)
    long countByUserIdInRange(@Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // Current-status count, for the active/stalled split.
    long countByUserIdAndStatusIn(Long userId, List<String> statuses);

    // NOTE: template/cvVariant use EXPLICIT LEFT JOINs (not the a.template.name /
    // a.cvVariant.name dot-path). Referencing an association via a dot-path in
    // JPQL causes Hibernate to generate an IMPLICIT INNER JOIN to resolve it,
    // regardless of the "IS NULL OR ..." guard around it — that inner join
    // happens at the FROM-clause level before the WHERE clause is evaluated.
    // That was silently excluding every Application with a null template or
    // null cvVariant from all filtered queries, even when the filter param
    // itself was null (i.e. "don't filter on this"). Using LEFT JOIN + the
    // join alias keeps rows with no associated template/cvVariant in the
    // result set.
    @Query("""
            SELECT a.id FROM Application a
            LEFT JOIN a.template t
            LEFT JOIN a.cvVariant cv
            WHERE a.user.id = :userId
            AND (CAST(:from AS timestamp) IS NULL OR a.dateApplied >= :from)
            AND (CAST(:to AS timestamp) IS NULL OR a.dateApplied <= :to)
            AND (CAST(:jobTitle AS string) IS NULL OR a.jobTitle = :jobTitle)
            AND (CAST(:template AS string) IS NULL OR t.name = :template)
            AND (CAST(:cvVariant AS string) IS NULL OR cv.name = :cvVariant)
            AND (CAST(:status AS string) IS NULL OR a.status = :status)
            AND (a.status IS NULL OR LOWER(a.status) <> 'compiled')
            """)
    List<Long> findApplicationIdsByUserIdAndFilters(@Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("jobTitle") String jobTitle,
            @Param("template") String template,
            @Param("cvVariant") String cvVariant,
            @Param("status") String status);

    @Query("""
            SELECT COUNT(a) FROM Application a
            LEFT JOIN a.template t
            LEFT JOIN a.cvVariant cv
            WHERE a.user.id = :userId
            AND (CAST(:from AS timestamp) IS NULL OR a.dateApplied >= :from)
            AND (CAST(:to AS timestamp) IS NULL OR a.dateApplied <= :to)
            AND (CAST(:jobTitle AS string) IS NULL OR a.jobTitle = :jobTitle)
            AND (CAST(:template AS string) IS NULL OR t.name = :template)
            AND (CAST(:cvVariant AS string) IS NULL OR cv.name = :cvVariant)
            AND (CAST(:status AS string) IS NULL OR a.status = :status)
            AND (a.status IS NULL OR LOWER(a.status) <> 'compiled')
            """)
    long countByUserIdAndFilters(@Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("jobTitle") String jobTitle,
            @Param("template") String template,
            @Param("cvVariant") String cvVariant,
            @Param("status") String status);

    @Query("""
            SELECT COUNT(a) FROM Application a
            LEFT JOIN a.template t
            LEFT JOIN a.cvVariant cv
            WHERE a.user.id = :userId
            AND (CAST(:from AS timestamp) IS NULL OR a.dateApplied >= :from)
            AND (CAST(:to AS timestamp) IS NULL OR a.dateApplied <= :to)
            AND (CAST(:jobTitle AS string) IS NULL OR a.jobTitle = :jobTitle)
            AND (CAST(:template AS string) IS NULL OR t.name = :template)
            AND (CAST(:cvVariant AS string) IS NULL OR cv.name = :cvVariant)
            AND (CAST(:status AS string) IS NULL OR a.status = :status)
            AND a.status IN :statuses
            AND LOWER(a.status) <> 'compiled'
            """)
    long countByUserIdAndStatusInFilters(@Param("userId") Long userId,
            @Param("statuses") List<String> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("jobTitle") String jobTitle,
            @Param("template") String template,
            @Param("cvVariant") String cvVariant,
            @Param("status") String status);

    // Lightweight list for the Kanban board and the timeline dropdown — no
    // pagination, no skills join, just enough to render a card.
    @Query("""
            SELECT new com.applyflow.tracker_api.dtos.ApplicationSummaryDto(
                a.id, a.companyName, a.jobTitle, a.status)
            FROM Application a
            WHERE a.user.id = :userId
            ORDER BY a.id DESC
            """)
    List<ApplicationSummaryDto> findSummariesByUserId(@Param("userId") Long userId);

    // Batch fetch for trend charts: id, dateApplied, and CURRENT status for
    // every application matching the filters within [from, to], bounded only
    // once for the whole requested range. StatsService.getTrendData buckets
    // these by dateApplied.toLocalDate() in memory instead of issuing one
    // findApplicationIdsByUserIdAndFilters + countByUserIdAndStatusInFilters
    // pair per day, which was the source of the O(days) query fan-out.
    @Query("""
            SELECT new com.applyflow.tracker_api.dtos.ApplicationBucketDto(a.id, a.dateApplied, a.status)
            FROM Application a
            LEFT JOIN a.template t
            LEFT JOIN a.cvVariant cv
            WHERE a.user.id = :userId
            AND a.dateApplied >= :from
            AND a.dateApplied <= :to
            AND (CAST(:jobTitle AS string) IS NULL OR a.jobTitle = :jobTitle)
            AND (CAST(:template AS string) IS NULL OR t.name = :template)
            AND (CAST(:cvVariant AS string) IS NULL OR cv.name = :cvVariant)
            AND (CAST(:status AS string) IS NULL OR a.status = :status)
            AND (a.status IS NULL OR LOWER(a.status) <> 'compiled')
            """)
    List<ApplicationBucketDto> findApplicationBucketDataByUserIdAndFilters(@Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("jobTitle") String jobTitle,
            @Param("template") String template,
            @Param("cvVariant") String cvVariant,
            @Param("status") String status);
}