package com.sequenceiq.environment.experience.liftie.responses;

import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "StatusMessage")
public class StatusMessage {

    private String message;

    private String status;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", StatusMessage.class.getSimpleName() + "[", "]")
                .add("message='" + message + "'")
                .add("status='" + status + "'")
                .toString();
    }
}
