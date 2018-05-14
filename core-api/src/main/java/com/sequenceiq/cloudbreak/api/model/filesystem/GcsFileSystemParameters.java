package com.sequenceiq.cloudbreak.api.model.filesystem;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class GcsFileSystemParameters implements FileSystemParameters {

    public static final String DEFAULT_BUCKET_NAME = "defaultBucketName";

    public static final String PROJECT_ID = "projectId";

    public static final String SERVICE_ACCOUNT_EMAIL = "serviceAccountEmail";

    private static final int PARAMETER_QUANTITY = 3;

    @ApiModelProperty
    private String defaultBucketName;

    @ApiModelProperty
    private String projectId;

    @ApiModelProperty
    private String serviceAccountEmail;

    public String getDefaultBucketName() {
        return defaultBucketName;
    }

    public void setDefaultBucketName(String defaultBucketName) {
        this.defaultBucketName = defaultBucketName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public void setServiceAccountEmail(String serviceAccountEmail) {
        this.serviceAccountEmail = serviceAccountEmail;
    }

    @Override
    public FileSystemType getType() {
        return FileSystemType.GCS;
    }

    @Override
    public Map<String, String> getAsMap() {
        Map<String, String> params = new LinkedHashMap<>(PARAMETER_QUANTITY);
        params.put(DEFAULT_BUCKET_NAME, defaultBucketName);
        params.put(PROJECT_ID, projectId);
        params.put(SERVICE_ACCOUNT_EMAIL, serviceAccountEmail);
        return params;
    }
}
