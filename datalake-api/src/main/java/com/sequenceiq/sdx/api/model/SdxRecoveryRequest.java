package com.sequenceiq.sdx.api.model;

import java.util.StringJoiner;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class SdxRecoveryRequest {

    @ApiModelProperty(ModelDescriptions.RECOVERY_TYPE)
    private SdxRecoveryType type;

    public SdxRecoveryType getType() {
        return type;
    }

    public void setType(SdxRecoveryType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", SdxRecoveryRequest.class.getSimpleName() + "[", "]")
                .add("type=" + type)
                .toString();
    }
}
