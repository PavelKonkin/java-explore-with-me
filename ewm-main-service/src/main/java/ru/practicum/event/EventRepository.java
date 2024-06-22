package ru.practicum.event;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {
    Optional<Event> findFirstByCategoryId(Long id);


    @EntityGraph(value = "event.category.location.user.likesAndDislikes")
    @Query(" select e from Event e where e.id = ?1 ")
    Optional<Event> findByIdWithGraph(Long id);

    @EntityGraph(value = "event.category.location.user.likesAndDislikes")
    List<Event> findAllByIdIn(List<Long> events);

    @EntityGraph(value = "event.category.location.user.likesAndDislikes",  type = EntityGraph.EntityGraphType.LOAD)
    List<Event> findAllByInitiatorId(long userId, Pageable page);

    @EntityGraph(value = "event.category.location.user.likesAndDislikes")
    Optional<Event> findByIdAndInitiatorId(long eventId, long userId);

    @EntityGraph(value = "event.category.location.user.likesAndDislikes")
    Optional<Event> findByIdAndState(long eventId, EventState eventState);
}
