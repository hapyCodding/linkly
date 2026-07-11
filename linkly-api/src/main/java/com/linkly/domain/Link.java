package com.linkly.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** 단축 링크 한 건. */
@Entity
@Table(
        name = "links",
        indexes = @Index(name = "idx_links_code", columnList = "code", unique = true))
public class Link {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Lob
    @Column(nullable = false)
    private String longUrl;

    @Column(length = 255)
    private String title;

    /** 개인 메모 (선택). */
    @Column(length = 500)
    private String memo;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column
    private Instant expiresAt;

    /** 비정규화된 클릭 카운터 (빠른 조회용). */
    @Column(nullable = false)
    private long clickCount = 0;

    /** 링크 정리용 태그 (link_tags 테이블). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "link_tags", joinColumns = @JoinColumn(name = "link_id"))
    @Column(name = "tag", length = 30)
    private Set<String> tags = new HashSet<>();

    protected Link() {
        // JPA 전용
    }

    public Link(String code, String longUrl, Instant expiresAt) {
        this.code = code;
        this.longUrl = longUrl;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public long getClickCount() {
        return clickCount;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }
}
