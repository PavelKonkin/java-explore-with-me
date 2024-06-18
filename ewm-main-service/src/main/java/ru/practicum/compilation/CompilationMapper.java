package ru.practicum.compilation;

import org.springframework.stereotype.Component;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.event.dto.EventShortDto;

import java.util.List;

@Component
public class CompilationMapper {
    public CompilationDto convertCompilation(Compilation compilation, List<EventShortDto> eventDtos) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.isPinned())
                .events(eventDtos)
                .build();
    }
}
