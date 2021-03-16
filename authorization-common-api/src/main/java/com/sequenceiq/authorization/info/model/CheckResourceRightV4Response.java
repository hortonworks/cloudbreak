package com.sequenceiq.authorization.info.model;

import io.swagger.annotations.ApiModel;

@ApiModel
public class CheckResourceRightV4Response {

    private String resourceCrn;

    private boolean result;

    public String getResourceCrn() {
        return resourceCrn;
    }

    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }
}
