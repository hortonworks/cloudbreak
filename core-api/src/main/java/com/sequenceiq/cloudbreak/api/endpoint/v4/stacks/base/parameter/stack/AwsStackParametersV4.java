package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import java.util.Map;

public class AwsStackParametersV4 extends StackParameterV4Base {

    private String awsS3Role;

    public String getAwsS3Role() {
        return awsS3Role;
    }

    public void setAwsS3Role(String awsS3Role) {
        this.awsS3Role = awsS3Role;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> map = super.asMap();
        map.put("awsS3Role", awsS3Role);
        return map;
    }

    @Override
    public <T> T toClass(Map<String, Object> parameters) {
        AwsStackParametersV4 ret = new AwsStackParametersV4();
        ret.awsS3Role = getParameterOrNull(parameters, "awsS3Role");
        ret.setTimeToLive(getTimeToLive(parameters));
        return (T) ret;
    }
}
