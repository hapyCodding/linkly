package com.linkly.repository;

/** 집계 결과 한 행 (라벨 + 개수) 프로젝션. */
public interface CountByLabel {
    String getLabel();

    long getCount();
}
