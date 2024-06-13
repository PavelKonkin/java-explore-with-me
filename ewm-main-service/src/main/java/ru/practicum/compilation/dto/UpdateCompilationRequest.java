package ru.practicum.compilation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class UpdateCompilationRequest {
    private List<Long> events;
    @Length(min = 1, max = 50)
    private String title;
    private Boolean pinned;
}
