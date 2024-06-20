package ru.practicum.event;

import java.time.LocalDateTime;

public interface EventParams {
    LocalDateTime getRangeStart();

    LocalDateTime getRangeEnd();

    Integer getFrom();

    Integer getSize();
}
