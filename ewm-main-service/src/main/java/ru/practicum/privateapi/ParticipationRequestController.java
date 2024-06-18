package ru.practicum.privateapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.participationrequest.dto.ParticipationRequestDto;

import java.time.LocalDateTime;
import java.util.List;

@RestController(value = "privateParticipationRequestController")
@RequestMapping("/users")
@Slf4j
public class ParticipationRequestController {
    private final ParticipationRequestService participationRequestService;

    @Autowired
    public ParticipationRequestController(@Qualifier(value = "privateParticipationRequestService") ParticipationRequestService participationRequestService) {
        this.participationRequestService = participationRequestService;
    }

    @PostMapping("/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto add(@PathVariable long userId, @RequestParam long eventId) {
        log.info("Получен запрос от пользователя с id {} на участие в событии с id {}", userId, eventId);
        ParticipationRequestDto result = participationRequestService.add(userId, eventId, LocalDateTime.now());
        log.info("Создан запрос на участие в событии {}", result);
        return result;
    }

    @GetMapping("/{userId}/requests")
    public List<ParticipationRequestDto> get(@PathVariable long userId) {
        log.info("Получен запрос на список запросов на участие в событиях от пользователя с id {}", userId);
        List<ParticipationRequestDto> result = participationRequestService.get(userId);
        log.info("Получен список запросов на участие {} в событиях пользователя с id {}", result, userId);
        return result;
    }

    @PatchMapping("/{userId}/requests/{requestId}/cancel")
    public ParticipationRequestDto cancel(@PathVariable long userId, @PathVariable long requestId) {
        log.info("Получен запрос от пользователя с id {} на отмену запроса с id {} участия в событии",
                userId, requestId);
        ParticipationRequestDto result = participationRequestService.cancel(userId, requestId);
        log.info("Запрос на участие {} в событии отменен ", result);
        return result;
    }
}
