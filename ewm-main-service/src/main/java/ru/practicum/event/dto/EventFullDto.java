package ru.practicum.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.event.EventState;
import ru.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder(toBuilder = true)
public class EventFullDto {
    private String annotation;
    private String description;
    private CategoryDto category;
    private long confirmedRequests;
    private LocalDateTime eventDate;
    private LocalDateTime createdOn;
    private LocalDateTime publishedOn;
    private long id;
    private UserShortDto initiator;
    private boolean paid;
    private boolean requestModeration;
    private String title;
    private int views;
    private int participantLimit;
    private LocationDto location;
    private EventState state;
}
