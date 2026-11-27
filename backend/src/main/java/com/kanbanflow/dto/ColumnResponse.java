package com.kanbanflow.dto;

import java.math.BigDecimal;
import java.util.List;

public record ColumnResponse(
        Long id,
        String title,
        String statusCode,
        BigDecimal position,
        List<CardResponse> cards) {
}
