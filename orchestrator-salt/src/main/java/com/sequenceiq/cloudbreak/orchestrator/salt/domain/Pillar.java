package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.Map;

public class Pillar {

    private String path;

    private Map<?, ?> json;

    public Pillar(String path, Map<?, ?> json) {
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

    public void setJson(Map<?, ?> json) {
        this.json = json;
    }
}
