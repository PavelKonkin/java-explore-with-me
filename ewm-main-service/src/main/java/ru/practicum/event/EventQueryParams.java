package ru.practicum.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.validator.DateComparisonConstraint;

import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@DateComparisonConstraint
public class EventQueryParams {
    private String text;

    private List<Long> categories = Collections.emptyList();
    private List<Long> users = Collections.emptyList();
    private List<EventState> states = Collections.emptyList();

    private Boolean paid;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rangeStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rangeEnd;

    private Boolean onlyAvailable = false;

    @Pattern(regexp = "EVENT_DATE|VIEWS")
    private String sort;

    @Min(0)
    private Integer from = 0;

    @Min(1)
    private Integer size = 10;

    public EventQueryParams() {
        this.text = null;
        this.categories = Collections.emptyList();
        this.users = Collections.emptyList();
        this.states = Collections.emptyList();
        this.paid = null;
        this.rangeStart = null;
        this.rangeEnd = null;
        this.onlyAvailable = false;
        this.sort = "EVENT_DATE";
        this.from = 0;
        this.size = 10;
    }
}
