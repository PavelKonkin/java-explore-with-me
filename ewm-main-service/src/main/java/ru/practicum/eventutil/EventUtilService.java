package ru.practicum.eventutil;

import java.util.List;
import java.util.Map;

public interface EventUtilService {
    Map<Long, Integer> getHitsByEvent(List<Long> eventIds);

    Map<Long, Long> getConfirmedRequestCountById(List<Long> eventIds);
}
