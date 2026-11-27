package com.kanbanflow.dto;

import java.time.LocalDate;

public record CardRequest(String title, String description, Long columnId, LocalDate dueDate) {
}
