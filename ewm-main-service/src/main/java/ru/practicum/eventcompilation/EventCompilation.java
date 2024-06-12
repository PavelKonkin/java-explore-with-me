package ru.practicum.eventcompilation;

import lombok.*;
import ru.practicum.compilation.Compilation;
import ru.practicum.event.Event;

import javax.persistence.*;
import java.util.Objects;

@Entity
@IdClass(EventCompilationId.class)
@Table(name = "event_compilation")
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class EventCompilation {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compilation_id")
    private Compilation compilation;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventCompilation eventCompilation = (EventCompilation) o;
        return Objects.equals(event.getId(), eventCompilation.event.getId())
                && Objects.equals(compilation.getId(), eventCompilation.compilation.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(event.getId(), compilation.getId());
    }
}

