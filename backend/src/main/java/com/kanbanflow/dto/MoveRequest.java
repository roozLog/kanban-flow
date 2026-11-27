package com.kanbanflow.dto;

import java.math.BigDecimal;

public record MoveRequest(Long toColumnId, BigDecimal position) {
}
