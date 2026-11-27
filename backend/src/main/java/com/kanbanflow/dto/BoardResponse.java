package com.kanbanflow.dto;

import java.util.List;

public record BoardResponse(Long id, String title, List<ColumnResponse> columns) {
}
