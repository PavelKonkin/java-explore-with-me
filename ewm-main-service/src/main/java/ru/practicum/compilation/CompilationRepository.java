package ru.practicum.compilation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    @EntityGraph(value = "compilation.event.initiator.location.category")
    List<Compilation> findAllByPinned(boolean pinned, Pageable pageable);
}
