package com.linkly.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;

/** 단축 링크 클릭 이벤트 1건 (분석용 로그). */
@Entity
@Table(
        name = "click_events",
        indexes = @Index(name = "idx_click_link_time", columnList = "link_id, clicked_at"))
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_id", nullable = false)
    private Long linkId;

    @Column(nullable = false)
    private Instant clickedAt = Instant.now();

    /** ISO 국가코드 추정치 (Accept-Language 기반, 미상이면 null). */
    @Column(length = 2)
    private String country;

    /** desktop / mobile / tablet / bot / unknown */
    @Column(length = 16)
    private String device;

    @Column(length = 255)
    private String referer;

    protected ClickEvent() {
        // JPA 전용
    }

    public ClickEvent(Long linkId, String country, String device, String referer) {
        this.linkId = linkId;
        this.country = country;
        this.device = device;
        this.referer = referer;
    }

    public Long getId() {
        return id;
    }

    public Long getLinkId() {
        return linkId;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }

    public String getCountry() {
        return country;
    }

    public String getDevice() {
        return device;
    }

    public String getReferer() {
        return referer;
    }
}
