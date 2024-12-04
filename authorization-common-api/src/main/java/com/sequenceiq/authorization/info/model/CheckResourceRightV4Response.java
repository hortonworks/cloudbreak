package com.sequenceiq.authorization.info.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class CheckResourceRightV4Response {

    private String resourceCrn;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
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
