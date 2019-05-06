package com.sequenceiq.environment.env.api.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "general status response")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WelcomeResponse implements Serializable {

    @ApiModelProperty("time")
    private String time;

    @ApiModelProperty("message")
    private String message;

    public WelcomeResponse(String message) {
        this(LocalDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME), message);
    }

    private WelcomeResponse(String time, String message) {
        this.time = time;
        this.message = message;
    }

    public WelcomeResponse() {
        time = LocalDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
