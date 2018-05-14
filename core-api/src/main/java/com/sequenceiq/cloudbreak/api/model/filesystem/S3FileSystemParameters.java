package com.sequenceiq.cloudbreak.api.model.filesystem;

import java.util.LinkedHashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.model.FileSystemType;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class S3FileSystemParameters implements FileSystemParameters {

    public static final String INSTANCE_PROFILE = "instanceProfile";

    private static final int PARAMETER_QUANTITY = 1;

    @ApiModelProperty
    private String instanceProfile;

    public String getInstanceProfile() {
        return instanceProfile;
    }

    public void setInstanceProfile(String instanceProfile) {
        this.instanceProfile = instanceProfile;
    }

    @Override
    public FileSystemType getType() {
        return FileSystemType.S3;
    }

    @Override
    public Map<String, String> getAsMap() {
        Map<String, String> params = new LinkedHashMap<>(PARAMETER_QUANTITY);
        params.put(INSTANCE_PROFILE, instanceProfile);
        return params;
    }
}
