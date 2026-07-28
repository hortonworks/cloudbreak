package com.sequenceiq.freeipa.flow.freeipa.verticalscale.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;

public class FreeIpaVerticalScaleParameters {

    private final String group;

    private final String targetInstanceType;

    private final String originalInstanceType;

    private final Integer rootVolumeSize;

    @JsonCreator
    public FreeIpaVerticalScaleParameters(
            @JsonProperty("group") String group,
            @JsonProperty("targetInstanceType") String targetInstanceType,
            @JsonProperty("originalInstanceType") String originalInstanceType,
            @JsonProperty("rootVolumeSize") Integer rootVolumeSize) {
        this.group = group;
        this.targetInstanceType = targetInstanceType;
        this.originalInstanceType = originalInstanceType;
        this.rootVolumeSize = rootVolumeSize;
    }

    public static FreeIpaVerticalScaleParameters fromRequest(VerticalScaleRequest request) {
        Integer rootVolSize = null;
        if (request.getTemplate().getRootVolume() != null) {
            rootVolSize = request.getTemplate().getRootVolume().getSize();
        }
        return new FreeIpaVerticalScaleParameters(
                request.getGroup(),
                request.getTemplate().getInstanceType(),
                null,
                rootVolSize);
    }

    public FreeIpaVerticalScaleParameters withOriginalInstanceType(String originalInstanceType) {
        return new FreeIpaVerticalScaleParameters(group, targetInstanceType, originalInstanceType, rootVolumeSize);
    }

    public String getGroup() {
        return group;
    }

    public String getTargetInstanceType() {
        return targetInstanceType;
    }

    public String getOriginalInstanceType() {
        return originalInstanceType;
    }

    public Integer getRootVolumeSize() {
        return rootVolumeSize;
    }

    @Override
    public String toString() {
        return "FreeIpaVerticalScaleParameters{" +
                "group='" + group + '\'' +
                ", targetInstanceType='" + targetInstanceType + '\'' +
                ", originalInstanceType='" + originalInstanceType + '\'' +
                ", rootVolumeSize=" + rootVolumeSize +
                '}';
    }
}
