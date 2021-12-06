package com.sequenceiq.freeipa.flow.stack.image.change.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

import reactor.rx.Promise;

public class ImageChangeEvent extends StackEvent {

    private final ImageSettingsRequest request;

    private String operationId;

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

    public ImageChangeEvent withOperationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ImageChangeEvent.class, other,
                event -> Objects.equals(operationId, event.operationId)
                        && Objects.equals(request, event.request));
    }

    @Override
    public String toString() {
        return "ImageChangeEvent{" +
                "request=" + request +
                ", operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}
