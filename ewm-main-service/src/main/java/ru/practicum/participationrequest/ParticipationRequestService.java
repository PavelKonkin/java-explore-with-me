package ru.practicum.participationrequest;

import ru.practicum.participationrequest.dto.ParticipationRequestDto;

import java.time.LocalDateTime;
import java.util.List;

public interface ParticipationRequestService {
    ParticipationRequestDto add(long userId, long eventId, LocalDateTime created);

    List<ParticipationRequestDto> get(long userId);

    ParticipationRequestDto cancel(long userId, long requestId);
}
