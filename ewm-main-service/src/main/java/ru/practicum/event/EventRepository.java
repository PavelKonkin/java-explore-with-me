package ru.practicum.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    Optional<Event> findFirstByCategoryId(Long id);

    @EntityGraph(value = "event.category.location.user")
    List<Event> findAllByIdIn(List<Long> events);

    @EntityGraph(value = "event.category.location.user")
    List<Event> findAllByInitiatorId(long userId, Pageable page);

    @EntityGraph(value = "event.category.location.user")
    Optional<Event> findByIdAndInitiatorId(long eventId, long userId);

    @EntityGraph(value = "event.category.location.user")
    Optional<Event> findByIdAndState(long eventId, EventState eventState);
}
