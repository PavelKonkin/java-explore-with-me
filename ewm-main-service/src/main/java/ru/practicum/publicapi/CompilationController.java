package ru.practicum.publicapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.page.OffsetPage;

import javax.validation.constraints.Min;
import java.util.List;

@RestController(value = "publicCompilationController")
@RequestMapping("/compilations")
@Slf4j
public class CompilationController {
    private final CompilationService compilationService;
    private final Sort sort = Sort.by("id").descending();

    @Autowired
    public CompilationController(@Qualifier(value = "publicCompilationService") CompilationService compilationService) {
        this.compilationService = compilationService;
    }

    @GetMapping
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

    @GetMapping("/{compId}")
    public CompilationDto get(@PathVariable long compId) {
        log.info("Получен запрос на подборку событий с id {}", compId);
        CompilationDto result = compilationService.get(compId);
        log.info("Получена подборка событий {}", result);
        return result;
    }
}
