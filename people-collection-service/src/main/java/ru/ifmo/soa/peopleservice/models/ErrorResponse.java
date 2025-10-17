package ru.ifmo.soa.peopleservice.models;

import java.time.OffsetDateTime;

public class ErrorResponse {
    private Integer code;
    private String message;
    private OffsetDateTime time;

    public ErrorResponse(Integer code, String message) {
        this.code = code;
        this.message = message;
        this.time = OffsetDateTime.now();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getTime() {
      return time;
    }

    public void setTime(OffsetDateTime time) {
      this.time = time;
    }
}
