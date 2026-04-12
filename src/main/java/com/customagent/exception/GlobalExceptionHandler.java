package com.customagent.exception;

import com.customagent.model.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Translates application exceptions into uniform JSON {@link ErrorResponse} payloads.
 *
 * <ul>
 *   <li>{@link AgentNotFoundException}            → 404 NOT_FOUND</li>
 *   <li>{@link AnalysisException}                 → 422 UNPROCESSABLE_ENTITY</li>
 *   <li>{@link IllegalArgumentException}          → 400 BAD_REQUEST</li>
 *   <li>{@link MethodArgumentNotValidException}   → 400 BAD_REQUEST (with field details)</li>
 *   <li>{@link Exception}                         → 500 INTERNAL_SERVER_ERROR</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AgentNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleAgentNotFound(AgentNotFoundException ex) {
        return new ErrorResponse("AGENT_NOT_FOUND", ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(AnalysisException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleAnalysisException(AnalysisException ex) {
        return new ErrorResponse("ANALYSIS_ERROR", ex.getMessage(),
                HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        return new ErrorResponse("INVALID_REQUEST", ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return new ErrorResponse("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex) {
        return new ErrorResponse("INTERNAL_ERROR",
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
