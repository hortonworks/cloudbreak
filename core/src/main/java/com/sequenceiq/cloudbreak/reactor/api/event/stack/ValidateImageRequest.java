package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

public class ValidateImageRequest<T> extends CloudPlatformRequest<ValidateImageResult> {

    private final StatedImage statedImage;

    private final CloudStack stack;

    private final Image image;

    public ValidateImageRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudStack stack, StatedImage statedImage, Image image) {
        super(cloudContext, cloudCredential);
        this.statedImage = statedImage;
        this.image = image;
        this.stack = stack;
    }

    public StatedImage getStatedImage() {
        return statedImage;
    }

    public Image getImage() {
        return image;
    }

    public CloudStack getStack() {
        return stack;
    }

    @Override
    public String toString() {
        return "ValidateImageRequest{" +
                "statedImage=" + statedImage +
                ", image=" + image +
                ", stack=" + stack +
                "} " + super.toString();
    }
}
