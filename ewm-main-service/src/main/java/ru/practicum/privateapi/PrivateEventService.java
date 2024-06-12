package ru.practicum.privateapi;

import org.springframework.data.domain.Pageable;
import ru.practicum.event.dto.*;
import ru.practicum.participationrequest.dto.ParticipationRequestDto;

import java.util.List;

public interface PrivateEventService {
    List<EventShortDto> getAll(long userId, Pageable page);

    EventFullDto add(long userId, NewEventDto newEventDto);

    EventFullDto get(long userId, long eventId);

    EventFullDto update(long userId, long eventId, UpdateEventUserRequest userRequest);

    List<ParticipationRequestDto> getRequests(long userId, long eventId);

    EventRequestStatusUpdateResult updateRequests(long userId, long eventId, EventRequestStatusUpdateRequest updateRequest);
}
