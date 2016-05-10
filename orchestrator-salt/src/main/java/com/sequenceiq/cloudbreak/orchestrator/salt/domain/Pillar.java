package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

public class Pillar {

    private String path;

    private Object json;

    public Pillar(String path, Object json) {
        this.path = path;
        this.json = json;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Object getJson() {
        return json;
    }

    public void setJson(Object json) {
        this.json = json;
    }
}
