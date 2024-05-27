package ru.practicum.hit;

import ru.practicum.HitDto;
import ru.practicum.ViewStatDto;

import java.time.LocalDateTime;
import java.util.List;

public interface HitService {
    void hits(HitDto hitDto);

    List<ViewStatDto> getStat(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
