package ru.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(BindException ex) {
        log.info("Ошибка валидации {}", ex.getMessage());
        BindingResult bindingResult = ex.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        String message = fieldErrors.stream()
                .map(item -> "Field: " + item.getField() + ". Error: " + item.getDefaultMessage()
                        + ". Value: " + item.getRejectedValue())
                .collect(Collectors.joining("\n "));
        List<String> errors = getStackTrace(ex);
        return new ApiError(errors,
                message,
                "Incorrectly made request.",
                HttpStatus.BAD_REQUEST.name(),
                LocalDateTime.now());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFoundException(NotFoundException ex) {
        log.info("Объект не найден " + ex.getMessage());
        List<String> errors = getStackTrace(ex);
        return new ApiError(errors,
                ex.getMessage(),
                "The required object was not found.",
                HttpStatus.NOT_FOUND.name(),
                LocalDateTime.now());

    }

    @ExceptionHandler({IllegalArgumentException.class, InvalidDataAccessApiUsageException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidationException(Throwable ex) {
        log.info("Ошибка валидации {}", ex.getMessage());
        List<String> errors = getStackTrace(ex);
        return new ApiError(errors,
                ex.getMessage(),
                "Incorrectly made request.",
                HttpStatus.BAD_REQUEST.name(),
                LocalDateTime.now());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConstraintViolationException(DataIntegrityViolationException ex) {
        log.info("Ошибка ограничения базы данных {}", ex.getMessage());
        List<String> errors = getStackTrace(ex);
        return new ApiError(errors,
                ex.getMessage(),
                "Integrity constraint has been violated.",
                HttpStatus.CONFLICT.name(),
                LocalDateTime.now());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleNumberFormatException(MethodArgumentTypeMismatchException ex) {
        log.info("Неверный формат аргумента {}", ex.getMessage());
        List<String> errors = getStackTrace(ex);
        return new ApiError(errors,
                ex.getMessage(),
                "Incorrectly made request.",
                HttpStatus.BAD_REQUEST.name(),
                LocalDateTime.now());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError handleConflictException(ConflictException ex) {
        log.info("Конфликт " + ex.getMessage());
        List<String> errors = getStackTrace(ex);
        return new ApiError(errors,
                ex.getMessage(),
                "For the requested operation the conditions are not met.",
                HttpStatus.CONFLICT.name(),
                LocalDateTime.now());

    }


    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleException(Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String stackTrace = sw.toString();
        log.error(stackTrace, ex);
        List<String> errors = getStackTrace(ex);
        return new ApiError(errors,
                ex.getMessage(),
                "The required object was not found.",
                HttpStatus.INTERNAL_SERVER_ERROR.name(),
                LocalDateTime.now());
    }

    private List<String> getStackTrace(Throwable ex) {
        List<String> errors = Arrays.stream(ex.getStackTrace())
                .map(StackTraceElement::toString)
                .collect(Collectors.toList());

        if (ex.getCause() != null) {
            errors.add("Cause: " + ex.getCause().toString());
        }
        return errors;
    }
}
