package ru.practicum.validator;

import ru.practicum.event.EventQueryParams;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

public class DateComparisonValidator implements ConstraintValidator<DateComparisonConstraint, EventQueryParams> {

    @Override
    public void initialize(DateComparisonConstraint constraint) {
    }

    @Override
    public boolean isValid(EventQueryParams model, ConstraintValidatorContext context) {
        LocalDateTime startDate = model.getRangeStart();
        LocalDateTime endDate = model.getRangeEnd();
        if (startDate == null || endDate == null) {
            return true;
        }

        return endDate.isAfter(startDate);
    }
}

