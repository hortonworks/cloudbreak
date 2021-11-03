package com.sequenceiq.sdx.api.model;

import java.util.StringJoiner;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class UpgradeRecoveryRequest {

    @ApiModelProperty(ModelDescriptions.RECOVERY_TYPE)
    private UpgradeRecoveryType type;

    public UpgradeRecoveryType getType() {
        return type;
    }

    public void setType(UpgradeRecoveryType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", UpgradeRecoveryRequest.class.getSimpleName() + "[", "]")
                .add("type=" + type)
                .toString();
    }
}
