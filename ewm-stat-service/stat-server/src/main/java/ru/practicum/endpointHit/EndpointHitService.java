package ru.practicum.endpointHit;

import ru.practicum.requestHit.dto.EndpointHitDto;
import ru.practicum.requestHit.dto.ViewStatDto;

import java.time.LocalDateTime;
import java.util.List;

public interface EndpointHitService {
    void hits(EndpointHitDto endpointHitDto);

    List<ViewStatDto> getStat(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
