package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PrepareUpgradeTriggerEvent extends StackEvent {

    private final String operationId;

    private final ImageSettingsRequest imageSettingsRequest;

    @JsonCreator
    public PrepareUpgradeTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("imageSettingsRequest") ImageSettingsRequest imageSettingsRequest) {
        super(selector, stackId);
        this.operationId = operationId;
        this.imageSettingsRequest = imageSettingsRequest;
    }

    public String getOperationId() {
        return operationId;
    }

    public ImageSettingsRequest getImageSettingsRequest() {
        return imageSettingsRequest;
    }

    @Override
    public String toString() {
        return "PrepareUpgradeTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                ", imageSettingsRequest=" + imageSettingsRequest +
                "} " + super.toString();
    }
}
