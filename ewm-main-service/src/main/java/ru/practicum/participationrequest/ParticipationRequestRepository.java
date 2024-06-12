package ru.practicum.participationrequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    List<ParticipationRequest> findAllByEventId(Long id);

    List<ParticipationRequest> findAllByRequesterId(long requesterId);

    Optional<ParticipationRequest> findByIdAndRequesterId(long requestId, long requesterId);

    List<ParticipationRequest> findAllByEventIdInAndStatus(List<Long> eventIds, ParticipationRequestStatus status);

    List<ParticipationRequest> findAllByEventIdAndEventInitiatorId(long eventId, long userId);

    List<ParticipationRequest> findAllByIdIn(List<Long> requestIds);

    List<ParticipationRequest> findAllByEventIdAndStatus(long eventId, ParticipationRequestStatus status);
}
