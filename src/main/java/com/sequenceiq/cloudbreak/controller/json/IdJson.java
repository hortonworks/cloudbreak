package com.sequenceiq.cloudbreak.controller.json;

public class IdJson implements JsonEntity {

    private Long id;

    public IdJson() {

    }

    public IdJson(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
