package com.kanbanflow.dto;

/** 카드가 속한 컬럼/보드 정보. */
public record CardInfoRow(Long id, Long columnId, Long boardId) {
}
