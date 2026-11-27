package com.kanbanflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CardResponse(
        Long id,
        Long columnId,
        String title,
        String description,
        BigDecimal position,
        LocalDate dueDate) {
}
