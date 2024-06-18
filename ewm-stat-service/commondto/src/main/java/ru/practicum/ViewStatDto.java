package ru.practicum;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewStatDto {
    private Long hits;
    private String app;
    private String uri;
}
