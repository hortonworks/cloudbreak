package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_IMAGE_EVENT;

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

    public ClusterUpgradeImageValidationEvent(Long resourceId, String imageId, CloudStack cloudStack, CloudCredential cloudCredential,
            CloudContext cloudContext, Image targetImage) {
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