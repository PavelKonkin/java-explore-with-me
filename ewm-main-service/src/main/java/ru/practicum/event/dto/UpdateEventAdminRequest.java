package ru.practicum.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springframework.format.annotation.DateTimeFormat;
import ru.practicum.validator.DateComparisonConstraint;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class UpdateEventAdminRequest {
    @Length(min = 20, max = 2000)
    private String annotation;
    @Length(min = 20, max = 7000)
    private String description;
    private Long category;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateComparisonConstraint(hoursCount = 1)
    private LocalDateTime eventDate;
    private Boolean paid;
    private Boolean requestModeration;
    @Length(min = 3, max = 120)
    private String title;
    private Integer participantLimit;
    private LocationDto location;
    private StateAdminAction stateAction;
}
