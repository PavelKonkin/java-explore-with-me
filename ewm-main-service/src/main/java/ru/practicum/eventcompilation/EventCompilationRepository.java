package ru.practicum.eventcompilation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventCompilationRepository extends JpaRepository<EventCompilation, EventCompilationId> {
    void deleteAllByCompilationId(long compId);

    @EntityGraph(value = "eventCompilation.event")
    List<EventCompilation> findAllByCompilationIdIn(List<Long> compilationIds);

    @EntityGraph(value = "eventCompilation.event")
    List<EventCompilation> findAllByCompilationId(long compId);
}
