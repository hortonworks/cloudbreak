package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

import reactor.rx.Promise;

public class ImageUpdateEvent extends StackEvent {

    private final StatedImage image;

    public ImageUpdateEvent(String selector, Long stackId, StatedImage image) {
        super(selector, stackId);
        this.image = image;
    }

    public ImageUpdateEvent(String selector, Long stackId, Promise<Boolean> accepted, StatedImage image) {
        super(selector, stackId, accepted);
        this.image = image;
    }

    public StatedImage getImage() {
        return image;
    }
}
