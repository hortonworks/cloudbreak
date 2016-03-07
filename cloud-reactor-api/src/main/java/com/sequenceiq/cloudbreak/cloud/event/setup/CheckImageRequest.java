package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;

public class CheckImageRequest<T> extends CloudPlatformRequest<CheckImageResult> {

    private final Image image;
    private final CloudStack stack;

    public CheckImageRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack stack, Image image) {
        super(cloudContext, cloudCredential);
        this.image = image;
        this.stack = stack;
    }

    public Image getImage() {
        return image;
    }

    public CloudStack getStack() {
        return stack;
    }
}
