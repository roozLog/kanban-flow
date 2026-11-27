package com.kanbanflow.dto;

import java.math.BigDecimal;

/** 컬럼 조회 결과 행 (카드 없이). */
public record ColumnRow(Long id, String title, String statusCode, BigDecimal position) {
}
