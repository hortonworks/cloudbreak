package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.util.Map;
import java.util.Set;

public class Pillar {

    private String path;

    private Map<?, ?> json;

    private Set<String> targets;

    public Pillar(String path, Map<?, ?> json, Set<String> targets) {
        this.path = path;
        this.json = json;
        this.targets = targets;
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

    public Set<String> getTargets() {
        return targets;
    }

    public void setTargets(Set<String> targets) {
        this.targets = targets;
    }

    @Override
    public String toString() {
        return "Pillar{" +
                "path='" + path + '\'' +
                ", json=" + json +
                ", targets=" + targets +
                '}';
    }
}
