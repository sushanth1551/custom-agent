package com.customagent.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Uniform error payload returned by the GlobalExceptionHandler. */
@Getter
@NoArgsConstructor
public class ErrorResponse {

    private String code;
    private String message;
    private int status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public ErrorResponse(String code, String message, int status) {
        this.code      = code;
        this.message   = message;
        this.status    = status;
        this.timestamp = LocalDateTime.now();
    }
}
