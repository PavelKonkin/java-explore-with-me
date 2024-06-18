package ru.practicum.adminapi;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;

public interface CompilationService {
    CompilationDto add(NewCompilationDto newCompilationDto);

    void delete(long compId);

    CompilationDto patch(long compId, UpdateCompilationRequest updateCompilationRequest);
}
