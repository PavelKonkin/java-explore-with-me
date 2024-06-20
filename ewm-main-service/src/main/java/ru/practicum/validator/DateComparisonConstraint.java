package ru.practicum.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateComparisonValidator.class)
public @interface DateComparisonConstraint {
    String message() default "Range end date must be after range start date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

