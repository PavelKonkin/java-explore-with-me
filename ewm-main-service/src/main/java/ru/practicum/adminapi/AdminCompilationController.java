package ru.practicum.adminapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;

import javax.validation.Valid;

@RestController
@RequestMapping("/admin/compilations")
@Slf4j
@Validated
public class AdminCompilationController {
    private final AdminCompilationService compilationService;

    @Autowired
    public AdminCompilationController(AdminCompilationService compilationService) {
        this.compilationService = compilationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto add(@Valid @RequestBody NewCompilationDto newCompilationDto) {
        log.info("Получен запрос на создание новой подборки событий {}", newCompilationDto);
        CompilationDto result = compilationService.add(newCompilationDto);
        log.info("Создана подборка событий {}", result);
        return result;
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long compId) {
        log.info("Получен запрос на удаление подборки событий с id {}", compId);
        compilationService.delete(compId);
        log.info("Удалена подборка событий с id {}", compId);
    }

    @PatchMapping("/{compId}")
    public CompilationDto patch(@Valid @RequestBody UpdateCompilationRequest updateCompilationRequest,
                                @PathVariable long compId) {
        log.info("Получен запрос на обновление подборки событий с id {}, данные для обновления {}",
                compId, updateCompilationRequest);
        CompilationDto result = compilationService.patch(compId, updateCompilationRequest);
        log.info("Создана подборка событий {}", result);
        return result;
    }
}
