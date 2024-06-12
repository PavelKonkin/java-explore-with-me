package ru.practicum.eventcompilation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventCompilationRepository extends JpaRepository<EventCompilation, EventCompilationId> {
    void deleteAllByCompilationId(long compId);

    List<EventCompilation> findAllByCompilationIdIn(List<Long> compilationIds);

    List<EventCompilation> findAllByCompilationId(long compId);
}
