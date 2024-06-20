package ru.practicum.publicapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.PublicEventParams;
import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.EventShortDto;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController(value = "publicEventController")
@RequestMapping("/events")
@Slf4j
@Validated
public class EventController {
    private final EventService eventService;

    @Autowired
    public EventController(@Qualifier(value = "publicEventService") EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<EventShortDto> getAll(@Valid @ModelAttribute PublicEventParams params,
                                      HttpServletRequest request) {
        log.info("Получен запрос на список событий, параметры запроса {}", params);
        List<EventShortDto> result = eventService.getAll(params,
                request.getRemoteAddr(), request.getRequestURI());
        log.info("Получен список событий {}", result);
        return result;
    }

    @GetMapping("/{eventId}")
    public EventFullDto get(@PathVariable long eventId, HttpServletRequest request) {
        log.info("Получен запрос на событие с id {}", eventId);
        EventFullDto result = eventService.get(eventId, request.getRemoteAddr(), request.getRequestURI());
        log.info("Получено событие {}", result);
        return result;
    }

}
