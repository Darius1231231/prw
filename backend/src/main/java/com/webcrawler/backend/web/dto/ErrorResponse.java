package com.webcrawler.backend.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public class ErrorResponse {

    private final String code;
    private final String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Instant timestamp = Instant.now();

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
