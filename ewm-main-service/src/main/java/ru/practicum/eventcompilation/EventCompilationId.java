package ru.practicum.eventcompilation;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventCompilationId implements Serializable {
    private Long event;
    private Long compilation;
}
