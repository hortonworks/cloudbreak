package com.sequenceiq.redbeams.api.endpoint.v4.database.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class EchoResponse {

    private String message;

    public EchoResponse() {
    }

    public EchoResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
