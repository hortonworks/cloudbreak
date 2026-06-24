package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PrepareUpgradeTriggerEvent extends StackEvent {

    private final String operationId;

    private final ImageSettingsRequest imageSettingsRequest;

    private final boolean needMigration;

    public PrepareUpgradeTriggerEvent(String selector, Long stackId, String operationId, ImageSettingsRequest imageSettingsRequest) {
        this(selector, stackId, operationId, imageSettingsRequest, false);
    }

    public PrepareUpgradeTriggerEvent(String selector, Long stackId, String operationId) {
        this(selector, stackId, operationId, null, false);
    }

    @JsonCreator
    public PrepareUpgradeTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("imageSettingsRequest") ImageSettingsRequest imageSettingsRequest,
            @JsonProperty("needMigration") boolean needMigration) {
        super(selector, stackId);
        this.operationId = operationId;
        this.imageSettingsRequest = imageSettingsRequest;
        this.needMigration = needMigration;
    }

    public String getOperationId() {
        return operationId;
    }

    public ImageSettingsRequest getImageSettingsRequest() {
        return imageSettingsRequest;
    }

    public boolean isNeedMigration() {
        return needMigration;
    }

    @Override
    public String toString() {
        return "PrepareUpgradeTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                ", imageSettingsRequest=" + imageSettingsRequest +
                ", needMigration=" + needMigration +
                "} " + super.toString();
    }
}
