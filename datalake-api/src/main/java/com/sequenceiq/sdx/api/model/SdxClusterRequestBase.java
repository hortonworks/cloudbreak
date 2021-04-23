package com.sequenceiq.sdx.api.model;

import java.util.HashMap;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.sequenceiq.common.api.tag.request.TaggableRequest;

public class SdxClusterRequestBase implements TaggableRequest {

    @NotNull
    private String environment;

    @NotNull
    private SdxClusterShape clusterShape;

    private SdxCloudStorageRequest cloudStorage;

    private SdxDatabaseRequest externalDatabase;

    @Valid
    private SdxAwsRequest aws;

    private Map<String, String> tags;

    private boolean enableRangerRaz;

    private boolean enableCMHA;

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

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    @Override
    public void addTag(String key, String value) {
        initAndGetTags().put(key, value);
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

    public SdxAwsRequest getAws() {
        return aws;
    }

    public void setAws(SdxAwsRequest aws) {
        this.aws = aws;
    }

    public boolean isEnableRangerRaz() {
        return enableRangerRaz;
    }

    public void setEnableRangerRaz(boolean enableRangerRaz) {
        this.enableRangerRaz = enableRangerRaz;
    }

    public boolean isEnableCMHA() {
        return enableCMHA;
    }

    public void setEnableCMHA(boolean enableCMHA) {
        this.enableCMHA = enableCMHA;
    }

    public void copyTo(SdxClusterRequestBase toInstance) {
        toInstance.setEnvironment(environment);
        toInstance.setClusterShape(clusterShape);
        toInstance.setTags(tags);
        toInstance.setCloudStorage(cloudStorage);
        toInstance.setExternalDatabase(externalDatabase);
        toInstance.setAws(aws);
        toInstance.setEnableRangerRaz(enableRangerRaz);
        toInstance.setEnableCMHA(enableCMHA);
    }

    @Override
    public String toString() {
        return "SdxClusterRequestBase{" +
                "clusterShape=" + clusterShape +
                ", cloudStorage=" + cloudStorage +
                ", externalDatabase=" + externalDatabase +
                ", aws=" + aws +
                ", enableRangerRaz=" + enableRangerRaz +
                ", enableCMHA=" + enableCMHA +
                '}';
    }
}
