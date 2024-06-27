package ru.practicum.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.event.dto.*;
import ru.practicum.page.OffsetPage;
import ru.practicum.participationrequest.dto.ParticipationRequestDto;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.List;

@RestController
@Slf4j
@Validated
public class EventController {
    private final EventService eventService;
    private final Sort sort = Sort.by("createdOn").descending();


    @Autowired
    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/users/{userId}/events")
    public List<EventShortDto> getAll(@PathVariable long userId,
                                      @RequestParam(defaultValue = "0") @Min(0) int from,
                                      @RequestParam(defaultValue = "10") @Min(1) int size) {
        log.info("Получен запрос на все события добавленные текущим пользователем с id {}," +
                " начиная с номера записи {}, количество записей {}", userId, from, size);
        Pageable page = new OffsetPage(from, size, sort);
        List<EventShortDto> result = eventService.getAll(userId, page);
        log.info("Для пользователя с id {} сформирован список событий {}", userId, result);
        return result;
    }

    @PostMapping("/users/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto post(@Valid @RequestBody NewEventDto newEventDto, @PathVariable long userId) {
        log.info("Получен запрс на создание нового события от пользователя с id {}, параметры события {}",
                userId, newEventDto);
        EventFullDto result = eventService.add(userId, newEventDto);
        log.info("Создано событие {}", result);
        return result;
    }

    @GetMapping("/users/{userId}/events/{eventId}")
    public EventFullDto get(@PathVariable long userId, @PathVariable long eventId) {
        log.info("Получен запрос на информацию о событии с id {} ползователя с id {}", eventId, userId);
        EventFullDto result = eventService.get(userId, eventId);
        log.info("Получено событие {}", result);
        return result;
    }

    @PatchMapping("/users/{userId}/events/{eventId}")
    public EventFullDto update(@Valid @RequestBody UpdateEventUserRequest userRequest,
                               @PathVariable long userId,
                               @PathVariable long eventId) {
        log.info("Получен запрос на изменение события с id {} от пользователя с id {}, данные для изменения {}",
                eventId, userId, userRequest);
        EventFullDto result = eventService.update(userId, eventId, userRequest);
        log.info("Изменено событие {}", result);
        return result;
    }

    @GetMapping("/users/{userId}/events/{eventId}/requests")
    public List<ParticipationRequestDto> getRequests(@PathVariable long userId, @PathVariable long eventId) {
        log.info("Получен запрос от пользователя с id {} на список запросов на участие в событии с id {}",
                userId, eventId);
        List<ParticipationRequestDto> result = eventService.getRequests(userId, eventId);
        log.info("Сформирован список запросов на участие в событии {}", result);
        return result;
    }

    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    public EventRequestStatusUpdateResult updateRequests(@RequestBody EventRequestStatusUpdateRequest updateRequest,
                                                         @PathVariable long userId,
                                                         @PathVariable long eventId) {
        log.info("Получен запрос на изменение статуса заявок на участие в событии с id {} пользователя с id {}," +
                " список заявок {}", eventId, userId, updateRequest);
        EventRequestStatusUpdateResult result = eventService.updateRequests(userId, eventId, updateRequest);
        log.info("Результат изменения статуса заявок {}", result);
        return result;
    }

    @GetMapping("/admin/events")
    public List<EventFullDto> getAll(@Valid @ModelAttribute AdminEventParams eventQueryParams) {
        log.info("Получен запрос от админа на список событий, параметры запроса {}", eventQueryParams);
        List<EventFullDto> result = eventService.getAll(eventQueryParams);
        log.info("Список событий получен {}", result);
        return result;
    }

    @PatchMapping("/admin/events/{eventId}")
    public EventFullDto update(@Valid @RequestBody UpdateEventAdminRequest updateRequest, @PathVariable long eventId) {
        log.info("Получен запрос на изменеие события с id {}, параметры для изменеия {}", eventId, updateRequest);
        EventFullDto result = eventService.patch(eventId, updateRequest);
        log.info("Событие изменено {}", result);
        return result;
    }

    @GetMapping("/events")
    public List<EventShortDto> getAll(@Valid @ModelAttribute PublicEventParams params,
                                      HttpServletRequest request) {
        log.info("Получен запрос на список событий, параметры запроса {}", params);
        List<EventShortDto> result = eventService.getAll(params,
                request.getRemoteAddr(), request.getRequestURI());
        log.info("Получен список событий {}", result);
        return result;
    }

    @GetMapping("/events/{eventId}")
    public EventFullDto get(@PathVariable long eventId, HttpServletRequest request) {
        log.info("Получен запрос на событие с id {}", eventId);
        EventFullDto result = eventService.get(eventId, request.getRemoteAddr(), request.getRequestURI());
        log.info("Получено событие {}", result);
        return result;
    }

    @PostMapping("/users/{userId}/events/{eventId}/like")
    public EventFullDto like(@PathVariable long userId, @PathVariable long eventId) {
        log.info("Получен запрос на отметку like события с id {} от пользователя с id {}", eventId, userId);
        EventFullDto result = eventService.like(userId, eventId);
        log.info("Поставлен like событию {}", result);
        return result;
    }

    @PatchMapping("/users/{userId}/events/{eventId}/remove_like")
    public EventFullDto removeLike(@PathVariable long userId, @PathVariable long eventId) {
        log.info("Получен запрос на отмену like события с id {} от пользователя с id {}", eventId, userId);
        EventFullDto result = eventService.removeLike(userId, eventId);
        log.info("Убран like с события {}", result);
        return result;
    }

    @PostMapping("/users/{userId}/events/{eventId}/dislike")
    public EventFullDto dislike(@PathVariable long userId, @PathVariable long eventId) {
        log.info("Получен запрос на отметку dislike события с id {} от пользователя с id {}", eventId, userId);
        EventFullDto result = eventService.dislike(userId, eventId);
        log.info("Поставлен dislike событию {}", result);
        return result;
    }

    @PatchMapping("/users/{userId}/events/{eventId}/remove_dislike")
    public EventFullDto removeDislike(@PathVariable long userId, @PathVariable long eventId) {
        log.info("Получен запрос на отмену dislike события с id {} от пользователя с id {}", eventId, userId);
        EventFullDto result = eventService.removeDislike(userId, eventId);
        log.info("Убран dislike события {}", result);
        return result;
    }
}
