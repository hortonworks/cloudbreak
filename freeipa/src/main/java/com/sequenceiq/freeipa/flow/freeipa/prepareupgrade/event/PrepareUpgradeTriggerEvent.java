package com.sequenceiq.freeipa.flow.freeipa.prepareupgrade.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class PrepareUpgradeTriggerEvent extends StackEvent {

    private final String operationId;

    private final ImageSettingsRequest imageSettingsRequest;

    private final boolean needMigration;

    private final boolean chained;

    private final boolean finalChain;

    public PrepareUpgradeTriggerEvent(String selector, Long stackId, String operationId, ImageSettingsRequest imageSettingsRequest) {
        this(selector, stackId, operationId, imageSettingsRequest, false, false, false);
    }

    @JsonCreator
    public PrepareUpgradeTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("imageSettingsRequest") ImageSettingsRequest imageSettingsRequest,
            @JsonProperty("needMigration") boolean needMigration,
            @JsonProperty("chained") boolean chained,
            @JsonProperty("finalChain") boolean finalChain) {
        super(selector, stackId);
        this.operationId = operationId;
        this.imageSettingsRequest = imageSettingsRequest;
        this.needMigration = needMigration;
        this.chained = chained;
        this.finalChain = finalChain;
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

    public boolean isChained() {
        return chained;
    }

    public boolean isFinalChain() {
        return finalChain;
    }

    @Override
    public String toString() {
        return "PrepareUpgradeTriggerEvent{" +
                "operationId='" + operationId + '\'' +
                ", imageSettingsRequest=" + imageSettingsRequest +
                ", needMigration=" + needMigration +
                ", chained=" + chained +
                ", finalChain=" + finalChain +
                "} " + super.toString();
    }
}
