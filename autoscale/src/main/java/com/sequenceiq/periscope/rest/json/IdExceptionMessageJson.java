package com.sequenceiq.periscope.rest.json;

import com.sequenceiq.periscope.api.model.Json;

public class IdExceptionMessageJson extends ExceptionMessageJson implements Json {

    private long id;

    public IdExceptionMessageJson() {
    }

    public IdExceptionMessageJson(long id, String message) {
        super(message);
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
