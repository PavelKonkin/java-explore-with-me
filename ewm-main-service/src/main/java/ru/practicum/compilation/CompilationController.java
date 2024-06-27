package ru.practicum.compilation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.page.OffsetPage;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

@RestController
@Slf4j
@Validated
public class CompilationController {
    private final CompilationService compilationService;
    private final Sort sort = Sort.by("id").descending();

    @Autowired
    public CompilationController(CompilationService compilationService) {
        this.compilationService = compilationService;
    }

    @PostMapping("/admin/compilations")
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto add(@Valid @RequestBody NewCompilationDto newCompilationDto) {
        log.info("Получен запрос на создание новой подборки событий {}", newCompilationDto);
        CompilationDto result = compilationService.add(newCompilationDto);
        log.info("Создана подборка событий {}", result);
        return result;
    }

    @DeleteMapping("/admin/compilations/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long compId) {
        log.info("Получен запрос на удаление подборки событий с id {}", compId);
        compilationService.delete(compId);
        log.info("Удалена подборка событий с id {}", compId);
    }

    @PatchMapping("/admin/compilations/{compId}")
    public CompilationDto patch(@Valid @RequestBody UpdateCompilationRequest updateCompilationRequest,
                                @PathVariable long compId) {
        log.info("Получен запрос на обновление подборки событий с id {}, данные для обновления {}",
                compId, updateCompilationRequest);
        CompilationDto result = compilationService.patch(compId, updateCompilationRequest);
        log.info("Создана подборка событий {}", result);
        return result;
    }

    @GetMapping("/compilations")
    public List<CompilationDto> getAll(@RequestParam(defaultValue = "false") boolean pinned,
                                       @RequestParam(defaultValue = "0") @Min(0) int from,
                                       @RequestParam(defaultValue = "10") @Min(1) int size) {
        log.info("Получен запрос на подборки событий начиная с записи {}, количество записей {}, закрепленные {}",
                from, size, pinned);
        Pageable page = new OffsetPage(from, size, sort);
        List<CompilationDto> result = compilationService.getAll(pinned, page);
        log.info("Получен список подборок событий {}", result);
        return result;
    }

    @GetMapping("/compilations/{compId}")
    public CompilationDto get(@PathVariable long compId) {
        log.info("Получен запрос на подборку событий с id {}", compId);
        CompilationDto result = compilationService.get(compId);
        log.info("Получена подборка событий {}", result);
        return result;
    }
}
