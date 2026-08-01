package ru.practicum.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(final NotFoundException e) {
        log.info("404 {}", e.getMessage());
        return new ErrorResponse(HttpStatus.NOT_FOUND, "The required object was not found.", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolationException(final DataIntegrityViolationException e) {
        log.warn("409: {}", e.getMessage());
        return new ErrorResponse(HttpStatus.CONFLICT, "Integrity constraint has been violated.", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConversionFailedException(final ConversionFailedException e) {
        log.warn("400: {}", e.getMessage());
        return new ErrorResponse(HttpStatus.BAD_REQUEST, "Incorrectly made request.", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintValidationException(ConstraintViolationException e) {
        return e.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> {
                    String message = String.format("Field: %s. Error: %s. Value: %s",
                            violation.getPropertyPath(),
                            violation.getMessage(),
                            violation.getInvalidValue());
                    log.warn("400: {}", message);
                    return new ErrorResponse(HttpStatus.BAD_REQUEST,
                            "Incorrectly made request.",
                            message,
                            LocalDateTime.now());
                })
                .orElseGet(() -> {
                    log.warn("400: ConstraintViolationException without violations: {}", e.getMessage());
                    return new ErrorResponse(HttpStatus.BAD_REQUEST,
                            "Incorrectly made request.",
                            "Validation failed",
                            LocalDateTime.now());
                });
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(final ValidationException e) {
        log.info("400 {}", e.getMessage());
        return new ErrorResponse(HttpStatus.BAD_REQUEST, "Ошибка валидации данных.", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(final ConflictException e) {
        log.warn("409 {}", e.getMessage());
        return new ErrorResponse(HttpStatus.CONFLICT, "For the requested operation the conditions are not met.", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorized(final NotAuthorized e) {
        log.warn("401 {}", e.getMessage());
        return new ErrorResponse(HttpStatus.UNAUTHORIZED, "User is not allowed to do this change.", e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAll(Exception e) {
        log.error("Unhandled exception", e);
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                e.getMessage(),
                LocalDateTime.now());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("400 Validation failed: {}", e.getMessage());
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                "Validation failed: " + e.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .collect(Collectors.joining("; ")),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("400: Malformed or missing request body: {}", e.getMessage());
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Request body is missing or malformed",
                e.getMessage(),
                LocalDateTime.now()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.warn("400: Missing required parameter: {}", e.getMessage());
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Incorrectly made request.",
                String.format("Required parameter '%s' of type %s is missing", e.getParameterName(), e.getParameterType()),
                LocalDateTime.now()
        );
    }
}