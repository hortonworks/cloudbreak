package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_IMAGE_EVENT;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationEvent;

public class ClusterUpgradeImageValidationEvent extends ClusterUpgradeValidationEvent {

    private final CloudCredential cloudCredential;

    private final CloudStack cloudStack;

    private final CloudContext cloudContext;

    private final Image targetImage;

    @JsonCreator
    public ClusterUpgradeImageValidationEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("imageId") String imageId,
            @JsonProperty("cloudStack") CloudStack cloudStack,
            @JsonProperty("cloudCredential") CloudCredential cloudCredential,
            @JsonProperty("cloudContext") CloudContext cloudContext,
            @JsonProperty("targetImage") Image targetImage) {
        super(VALIDATE_IMAGE_EVENT.selector(), resourceId, imageId);
        this.cloudStack = cloudStack;
        this.cloudCredential = cloudCredential;
        this.cloudContext = cloudContext;
        this.targetImage = targetImage;
    }

    public CloudCredential getCloudCredential() {
        return cloudCredential;
    }

    public CloudStack getCloudStack() {
        return cloudStack;
    }

    public CloudContext getCloudContext() {
        return cloudContext;
    }

    public Image getTargetImage() {
        return targetImage;
    }

    @Override
    public String toString() {
        return "ClusterUpgradeImageValidationEvent{" +
                "cloudCredential=" + cloudCredential +
                ", cloudStack=" + cloudStack +
                ", cloudContext=" + cloudContext +
                "} " + super.toString();
    }
}
