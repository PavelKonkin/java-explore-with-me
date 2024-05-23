package ru.practicum.requestHit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewStatDto {
    private String app;
    private String uri;
    private Integer hits;
}
