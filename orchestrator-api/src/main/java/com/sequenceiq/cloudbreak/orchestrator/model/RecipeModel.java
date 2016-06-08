package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeModel {

    private String name;
    private List<String> preInstall = new ArrayList<>();
    private List<String> postInstall = new ArrayList<>();
    private Map<String, String> keyValues = new HashMap<>();

    public RecipeModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPreInstall() {
        return preInstall;
    }

    public void addPreInstall(String script) {
        this.preInstall.add(script);
    }

    public List<String> getPostInstall() {
        return postInstall;
    }

    public void addPostInstall(String script) {
        this.postInstall.add(script);
    }

    public Map<String, String> getKeyValues() {
        return keyValues;
    }

    public void setKeyValues(Map<String, String> keyValues) {
        this.keyValues = keyValues;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RecipeModel{");
        sb.append(", name='").append(name).append('\'');
        sb.append(", preInstall=").append(preInstall);
        sb.append(", postInstall=").append(postInstall);
        sb.append(", keyValues=").append(keyValues);
        sb.append('}');
        return sb.toString();
    }
}
