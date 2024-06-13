package ru.practicum.participationrequest;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    @EntityGraph(value = "participationRequest.user.event")
    List<ParticipationRequest> findAllByRequesterId(long requesterId);

    Optional<ParticipationRequest> findByIdAndRequesterId(long requestId, long requesterId);

    @EntityGraph(value = "participationRequest.event")
    List<ParticipationRequest> findAllByEventIdInAndStatus(List<Long> eventIds, ParticipationRequestStatus status);

    @EntityGraph(value = "participationRequest.user.event")
    List<ParticipationRequest> findAllByEventIdAndEventInitiatorId(long eventId, long userId);

    @EntityGraph(value = "participationRequest.user.event")
    List<ParticipationRequest> findAllByIdIn(List<Long> requestIds);

    List<ParticipationRequest> findAllByEventIdAndStatus(long eventId, ParticipationRequestStatus status);
}
