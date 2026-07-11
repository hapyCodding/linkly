package com.linkly.repository;

import com.linkly.domain.Link;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<Link, Long> {

    Optional<Link> findByCode(String code);

    boolean existsByCode(String code);

    /** 리다이렉트 핫패스: 카운터를 원자적으로 증가시켜 경합을 피한다. */
    @Modifying
    @Query("update Link l set l.clickCount = l.clickCount + 1 where l.id = :id")
    void incrementClickCount(@Param("id") Long id);
}
