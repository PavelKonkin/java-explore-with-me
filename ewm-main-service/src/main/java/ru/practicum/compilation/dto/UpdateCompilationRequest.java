package ru.practicum.compilation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class UpdateCompilationRequest {
    private List<Long> events;
    private String title;
    private Boolean pinned;
}
