package ru.practicum.hit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.HitDto;
import ru.practicum.ViewStatDto;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
@Validated
public class HitController {
    private final HitService hitService;

    @Autowired
    public HitController(HitService hitService) {
        this.hitService = hitService;
    }

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void hit(@Valid @RequestBody HitDto hitDto) {
        log.info("Получен запрос на регистрацию запроса к эндпойнту {}", hitDto);
        hitService.hits(hitDto);
    }

    @GetMapping("/stats")
    public List<ViewStatDto> getStats(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                      @RequestParam(required = false) List<String> uris,
                                      @RequestParam(required = false, defaultValue = "false") boolean unique) {
        log.info("Получен запрос на статистику с даты {} по дату {} для адресов {}, только уникальные ip - {}",
                start, end, uris, unique);
        List<ViewStatDto> result = hitService.getStat(start, end, uris, unique);
        log.info("Получена статистика вызовов эндпойнтов {}", result);
        return result;
    }
}
