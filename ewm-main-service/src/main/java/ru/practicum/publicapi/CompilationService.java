package ru.practicum.publicapi;

import org.springframework.data.domain.Pageable;
import ru.practicum.compilation.dto.CompilationDto;

import java.util.List;

public interface CompilationService {
    List<CompilationDto> getAll(boolean pinned, Pageable page);

    CompilationDto get(long compId);
}
