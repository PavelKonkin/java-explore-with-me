package ru.practicum.hit;

import org.springframework.stereotype.Component;
import ru.practicum.HitDto;

@Component
public class HitMapper {
    public Hit toModel(HitDto hitDto) {
        return Hit.builder()
                .app(hitDto.getApp())
                .ip(hitDto.getIp())
                .uri(hitDto.getUri())
                .timestamp(hitDto.getTimestamp())
                .build();
    }
}
