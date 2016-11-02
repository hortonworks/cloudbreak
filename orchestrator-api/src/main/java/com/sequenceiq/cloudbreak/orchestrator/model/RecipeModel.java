package com.sequenceiq.cloudbreak.orchestrator.model;

import java.util.HashMap;
import java.util.Map;

public class RecipeModel {

    private String name;

    private String preInstall;

    private String postInstall;

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

    public String getPreInstall() {
        return preInstall;
    }

    public void setPreInstall(String preInstall) {
        this.preInstall = preInstall;
    }

    public String getPostInstall() {
        return postInstall;
    }

    public void setPostInstall(String postInstall) {
        this.postInstall = postInstall;
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
