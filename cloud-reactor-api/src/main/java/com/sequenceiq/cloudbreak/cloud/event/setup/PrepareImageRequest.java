package com.sequenceiq.cloudbreak.cloud.event.setup;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.catalog.PrepareImageType;

public class PrepareImageRequest<T> extends CloudPlatformRequest<PrepareImageResult> {

    private final Image image;

    private final CloudStack stack;

    private final PrepareImageType prepareImageType;

    private final String imageFallbackTarget;

    public PrepareImageRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack stack, Image image, PrepareImageType prepareImageType) {
        super(cloudContext, cloudCredential);
        this.image = image;
        this.stack = stack;
        this.prepareImageType = prepareImageType;
        this.imageFallbackTarget = null;
    }

    public PrepareImageRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack stack, Image image, PrepareImageType prepareImageType,
            String imageFallbackTarget) {
        super(cloudContext, cloudCredential);
        this.image = image;
        this.stack = stack;
        this.prepareImageType = prepareImageType;
        this.imageFallbackTarget = imageFallbackTarget;
    }

    public Image getImage() {
        return image;
    }

    public CloudStack getStack() {
        return stack;
    }

    public PrepareImageType getPrepareImageType() {
        return prepareImageType;
    }

    public String getImageFallbackTarget() {
        return imageFallbackTarget;
    }

    @Override
    public String toString() {
        return "PrepareImageRequest{" +
                "image=" + image +
                ", stack=" + stack +
                ", prepareImageType=" + prepareImageType +
                ", imageFallbackTarget='" + imageFallbackTarget + '\'' +
                "} " + super.toString();
    }
}
