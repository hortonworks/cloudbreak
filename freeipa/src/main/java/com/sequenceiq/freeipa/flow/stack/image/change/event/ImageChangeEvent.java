package com.sequenceiq.freeipa.flow.stack.image.change.event;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class ImageChangeEvent extends StackEvent {

    private final ImageSettingsRequest request;

    public ImageChangeEvent(Long stackId, ImageSettingsRequest request) {
        super(stackId);
        this.request = request;
    }

    public ImageChangeEvent(String selector, Long stackId, ImageSettingsRequest request) {
        super(selector, stackId);
        this.request = request;
    }

    public ImageChangeEvent(String selector, Long stackId, Promise<AcceptResult> accepted, ImageSettingsRequest request) {
        super(selector, stackId, accepted);
        this.request = request;
    }

    public ImageSettingsRequest getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return "ImageChangeEvent{" +
                "super: " + super.toString() +
                "request=" + request +
                '}';
    }
}
