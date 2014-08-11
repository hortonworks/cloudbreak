package com.sequenceiq.periscope.rest.json;

public class IdJson implements Json {

    private String id;

    public IdJson() {
    }

    public IdJson(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public static IdJson emptyJson() {
        return new IdJson("");
    }
}
