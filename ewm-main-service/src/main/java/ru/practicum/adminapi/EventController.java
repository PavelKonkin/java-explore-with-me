package ru.practicum.adminapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.AdminEventParams;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventAdminRequest;

import javax.validation.Valid;
import java.util.List;

@RestController(value = "adminEventController")
@RequestMapping("/admin/events")
@Slf4j
@Validated
public class EventController {

    private final EventService eventService;

    @Autowired
    public EventController(@Qualifier(value = "adminEventService") EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventFullDto> getAll(@Valid @ModelAttribute AdminEventParams eventQueryParams) {
        log.info("Получен запрос от админа на список событий, параметры запроса {}", eventQueryParams);
        List<EventFullDto> result = eventService.getAll(eventQueryParams);
        log.info("Список событий получен {}", result);
        return result;
    }

    @PatchMapping("/{eventId}")
    public EventFullDto update(@Valid @RequestBody UpdateEventAdminRequest updateRequest, @PathVariable long eventId) {
        log.info("Получен запрос на изменеие события с id {}, параметры для изменеия {}", eventId, updateRequest);
        EventFullDto result = eventService.patch(eventId, updateRequest);
        log.info("Событие изменено {}", result);
        return result;
    }


}
