package com.sequenceiq.sdx.api.model;

import java.util.StringJoiner;

public class SdxRecoveryRequest {

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
