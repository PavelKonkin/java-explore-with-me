package ru.practicum.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    Optional<Event> findFirstByCategoryId(Long id);

    List<Event> findAllByIdIn(List<Long> events);

    List<Event> findAllByInitiatorId(long userId, Pageable page);

    Optional<Event> findByIdAndInitiatorId(long eventId, long userId);
}
