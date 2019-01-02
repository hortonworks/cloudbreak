package com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack;

import java.util.HashMap;
import java.util.Map;

import io.swagger.annotations.ApiModelProperty;

public class AzureStackV4Parameters extends StackV4ParameterBase {

    @ApiModelProperty
    private String resourceGroupName;

    @ApiModelProperty
    private boolean encryptStorage;

    @ApiModelProperty
    private boolean yarnQueue;

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public boolean isEncryptStorage() {
        return encryptStorage;
    }

    public void setEncryptStorage(boolean encryptStorage) {
        this.encryptStorage = encryptStorage;
    }

    public boolean isYarnQueue() {
        return yarnQueue;
    }

    public void setYarnQueue(boolean yarnQueue) {
        this.yarnQueue = yarnQueue;
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> ret = new HashMap<>();
        ret.put("resourceGroupName", resourceGroupName);
        ret.put("encryptStorage", encryptStorage);
        ret.put("yarnQueue", yarnQueue);
        return ret;
    }

    @Override
    public void parse(Map<String, Object> parameters) {
        resourceGroupName = getParameterOrNull(parameters, "resourceGroupName");
        encryptStorage = getBoolean(parameters, "encryptStorage");
        yarnQueue = getBoolean(parameters, "yarnQueue");
    }
}
