package ru.practicum.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RatingDto {
    private Long id;
    private Long rating;
}
