package ru.practicum.endpointHit;

import org.springframework.stereotype.Component;
import ru.practicum.requestHit.dto.EndpointHitDto;

@Component
public class EndpointHitMapper {
    public EndpointHit toModel(EndpointHitDto endpointHitDto) {
        return EndpointHit.builder()
                .app(endpointHitDto.getApp())
                .ip(endpointHitDto.getIp())
                .uri(endpointHitDto.getUri())
                .timestamp(endpointHitDto.getTimestamp())
                .build();
    }
}
