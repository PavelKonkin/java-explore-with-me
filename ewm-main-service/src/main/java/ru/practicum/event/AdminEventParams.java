package ru.practicum.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.validator.DateComparisonConstraint;

import javax.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@DateComparisonConstraint
public class AdminEventParams implements EventParams {
    private List<Long> categories = Collections.emptyList();
    private List<Long> users = Collections.emptyList();
    private List<EventState> states = Collections.emptyList();


    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rangeStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rangeEnd;

    @Min(0)
    private Integer from = 0;

    @Min(1)
    private Integer size = 10;

    public AdminEventParams() {
        this.categories = Collections.emptyList();
        this.users = Collections.emptyList();
        this.states = Collections.emptyList();
        this.rangeStart = null;
        this.rangeEnd = null;
        this.from = 0;
        this.size = 10;
    }
}
