package ru.practicum.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;

public class DateInFutureByHoursValidator implements ConstraintValidator<DateInFutureByHoursConstraint, LocalDateTime> {

    private int hoursCount;

    @Override
    public void initialize(DateInFutureByHoursConstraint constraint) {
            this.hoursCount = constraint.hoursCount();
    }

    @Override
    public boolean isValid(LocalDateTime value, ConstraintValidatorContext context) {

        if (value == null) {
            return true;
        }

        return value.isAfter(LocalDateTime.now().plusHours(hoursCount));
    }
}

