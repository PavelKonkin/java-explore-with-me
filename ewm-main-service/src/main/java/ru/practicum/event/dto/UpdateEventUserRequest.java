package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import ru.practicum.validator.DateInFutureByHoursConstraint;

import javax.validation.constraints.PositiveOrZero;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateEventUserRequest {
    @Length(min = 20, max = 2000)
    private String annotation;
    @Length(min = 20, max = 7000)
    private String description;
    private Long category;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd" + " " + "HH:mm:ss")
    @DateInFutureByHoursConstraint(hoursCount = 2)
    private LocalDateTime eventDate;
    private Boolean paid;
    private Boolean requestModeration;
    @Length(min = 3, max = 120)
    private String title;
    @PositiveOrZero
    private Integer participantLimit;
    private LocationDto location;
    private StateUserAction stateAction;
}
