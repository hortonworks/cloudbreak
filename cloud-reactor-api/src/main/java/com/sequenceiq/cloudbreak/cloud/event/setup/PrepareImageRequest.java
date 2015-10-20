package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Image;

public class PrepareImageRequest<T> extends CloudPlatformRequest<PrepareImageResult> {

    private final Image image;

    public PrepareImageRequest(CloudContext cloudContext, CloudCredential cloudCredential, Image image) {
        super(cloudContext, cloudCredential);
        this.image = image;
    }

    public Image getImage() {
        return image;
    }
}
