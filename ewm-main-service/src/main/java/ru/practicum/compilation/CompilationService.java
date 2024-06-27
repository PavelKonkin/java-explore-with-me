package ru.practicum.compilation;

import org.springframework.data.domain.Pageable;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {
    CompilationDto add(NewCompilationDto newCompilationDto);

    void delete(long compId);

    CompilationDto patch(long compId, UpdateCompilationRequest updateCompilationRequest);

    List<CompilationDto> getAll(boolean pinned, Pageable page);

    CompilationDto get(long compId);
}
