package com.sequenceiq.provisioning.controller.json;


public class ExceptionJson {

    private String message;

    public ExceptionJson(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
