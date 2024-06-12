package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import ru.practicum.validator.DateComparisonConstraint;

import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class NewEventDto {
    @NotBlank
    @Length(min = 20, max = 2000)
    private String annotation;
    @NotBlank
    @Length(min = 20, max = 7000)
    private String description;
    private long category;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @DateComparisonConstraint(hoursCount = 2)
    private LocalDateTime eventDate;
    private boolean paid;
    private boolean requestModeration;
    @NotBlank
    @Length(min = 3, max = 120)
    private String title;
    @PositiveOrZero
    private int participantLimit;
    @NotNull
    private LocationDto location;

    public NewEventDto() {
        this.paid = false;
        this.requestModeration = false;
        this.participantLimit = 0;
    }
}
