package com.sequenceiq.periscope.rest.json;

public class IdExceptionMessageJson extends ExceptionMessageJson implements Json {

    private String id;

    public IdExceptionMessageJson() {
    }

    public IdExceptionMessageJson(String id, String message) {
        super(message);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
