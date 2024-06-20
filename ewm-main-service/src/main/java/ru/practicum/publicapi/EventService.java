package ru.practicum.publicapi;

import ru.practicum.event.PublicEventParams;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;

import java.util.List;

public interface EventService {
    List<EventShortDto> getAll(PublicEventParams params, String ipAddress, String uri);

    EventFullDto get(long eventId, String remoteAddr, String requestURI);
}
