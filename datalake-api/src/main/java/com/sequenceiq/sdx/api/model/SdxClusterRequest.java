package com.sequenceiq.sdx.api.model;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.sequenceiq.authorization.api.EnvironmentNameAwareApiModel;

public class SdxClusterRequest implements EnvironmentNameAwareApiModel {

    @NotNull
    private String environment;

    @NotNull
    private SdxClusterShape clusterShape;

    private SdxCloudStorageRequest cloudStorage;

    private SdxDatabaseRequest externalDatabase;

    private Map<String, String> tags;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public SdxClusterShape getClusterShape() {
        return clusterShape;
    }

    public void setClusterShape(SdxClusterShape clusterShape) {
        this.clusterShape = clusterShape;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public Map<String, String> initAndGetTags() {
        if (tags == null) {
            tags = new HashMap<>();
        }
        return tags;
    }

    public void addTags(Map<String, String> tags) {
        initAndGetTags().putAll(tags);
    }

    public SdxCloudStorageRequest getCloudStorage() {
        return cloudStorage;
    }

    public void setCloudStorage(SdxCloudStorageRequest cloudStorage) {
        this.cloudStorage = cloudStorage;
    }

    public SdxDatabaseRequest getExternalDatabase() {
        return externalDatabase;
    }

    public void setExternalDatabase(SdxDatabaseRequest externalDatabase) {
        this.externalDatabase = externalDatabase;
    }

    @Override
    public String getEnvironmentName() {
        return environment;
    }
}
