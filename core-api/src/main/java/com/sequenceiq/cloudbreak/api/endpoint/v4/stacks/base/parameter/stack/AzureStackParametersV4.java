package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import java.util.HashMap;
import java.util.Map;

import io.swagger.annotations.ApiModelProperty;

public class AzureStackParametersV4 extends StackParameterV4Base {

    @ApiModelProperty
    private String resourceGroupName;

    @ApiModelProperty
    private boolean encryptStorage;

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("resourceGroupName", resourceGroupName);
        ret.put("encryptStorage", encryptStorage);
        return ret;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        resourceGroupName = getParameterOrNull(parameters, "resourceGroupName");
        encryptStorage = getBoolean(parameters, "encryptStorage");
    }
}
