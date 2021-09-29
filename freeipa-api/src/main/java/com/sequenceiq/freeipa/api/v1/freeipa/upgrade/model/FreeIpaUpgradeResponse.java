package com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.ApiModel;

@ApiModel("FreeIpaUpgradeV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaUpgradeResponse {

    private FlowIdentifier flowIdentifier;

    private ImageInfoResponse targetImage;

    private ImageInfoResponse originalImage;

    private String operationId;

    public FreeIpaUpgradeResponse(FlowIdentifier flowIdentifier, ImageInfoResponse targetImage, ImageInfoResponse originalImage, String operationId) {
        this.flowIdentifier = flowIdentifier;
        this.targetImage = targetImage;
        this.originalImage = originalImage;
        this.operationId = operationId;
    }

    public FreeIpaUpgradeResponse() {
    }

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public ImageInfoResponse getTargetImage() {
        return targetImage;
    }

    public void setTargetImage(ImageInfoResponse targetImage) {
        this.targetImage = targetImage;
    }

    public ImageInfoResponse getOriginalImage() {
        return originalImage;
    }

    public void setOriginalImage(ImageInfoResponse originalImage) {
        this.originalImage = originalImage;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    @Override
    public String toString() {
        return "FreeIpaUpgradeResponse{" +
                "flowIdentifier=" + flowIdentifier +
                ", targetImage=" + targetImage +
                ", originalImage=" + originalImage +
                ", operationId='" + operationId + '\'' +
                '}';
    }
}
