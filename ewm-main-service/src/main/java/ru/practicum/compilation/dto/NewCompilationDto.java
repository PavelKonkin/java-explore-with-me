package ru.practicum.compilation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class NewCompilationDto {
    private List<Long> events;
    @NotBlank
    @Length(min = 1, max = 50)
    private String title;
    private boolean pinned;
}
