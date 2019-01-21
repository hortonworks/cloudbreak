package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.api.endpoint.v4.JsonEntity;

public class AwsStackParametersV4 implements JsonEntity, Mappable {

    private String awsS3Role;

    public String getAwsS3Role() {
        return awsS3Role;
    }

    public void setAwsS3Role(String awsS3Role) {
        this.awsS3Role = awsS3Role;
    }

    @Override
    public Map<String, Object> asMap() {
        return new HashMap<>();
    }

    @Override
    public <T> T toClass(Map<String, Object> parameters) {
        return null;
    }
}
