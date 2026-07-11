package com.linkly.repository;

import com.linkly.domain.ClickEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByLinkId(Long linkId);

    /** 링크 삭제 시 해당 클릭 이벤트를 한 번에 제거. */
    @Modifying
    @Query("delete from ClickEvent c where c.linkId = :linkId")
    void deleteByLinkId(@Param("linkId") Long linkId);

    List<ClickEvent> findTop20ByLinkIdOrderByClickedAtDesc(Long linkId);

    @Query("""
            select coalesce(c.country, 'ZZ') as label, count(c) as count
            from ClickEvent c
            where c.linkId = :linkId
            group by c.country
            order by count desc
            """)
    List<CountByLabel> countByCountry(@Param("linkId") Long linkId);

    @Query("""
            select coalesce(c.device, 'unknown') as label, count(c) as count
            from ClickEvent c
            where c.linkId = :linkId
            group by c.device
            order by count desc
            """)
    List<CountByLabel> countByDevice(@Param("linkId") Long linkId);

    /** 최근 클릭 이벤트 시각 목록 (시간대별 추이 집계는 서비스 레이어에서 버킷팅). */
    @Query("select c.clickedAt from ClickEvent c where c.linkId = :linkId and c.clickedAt >= :since")
    List<Instant> clickTimestampsSince(@Param("linkId") Long linkId, @Param("since") Instant since);
}
