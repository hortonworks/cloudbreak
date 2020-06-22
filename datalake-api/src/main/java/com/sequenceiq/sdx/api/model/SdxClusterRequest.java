package com.sequenceiq.sdx.api.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.sequenceiq.common.api.tag.base.TagsBase;
import com.sequenceiq.common.api.tag.request.TaggableRequest;
import com.sequenceiq.common.api.tag.request.TagsRequest;

public class SdxClusterRequest implements TaggableRequest {

    @NotNull
    private String environment;

    @NotNull
    private SdxClusterShape clusterShape;

    private String runtime;

    private SdxCloudStorageRequest cloudStorage;

    private SdxDatabaseRequest externalDatabase;

    @Valid
    private SdxAwsRequest aws;

    private TagsRequest tags = new TagsRequest();

    private boolean enableRangerRaz;

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

    public String getRuntime() {
        return runtime;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public void setTags(TagsRequest tags) {
        this.tags = tags;
    }

    public TagsRequest getTags() {
        return tags;
    }

    @Override
    public void addTag(String key, String value) {
        tags.addTag(key, value);
    }

    public void addTags(TagsBase tags) {
        tags.addTags(tags);
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
}
