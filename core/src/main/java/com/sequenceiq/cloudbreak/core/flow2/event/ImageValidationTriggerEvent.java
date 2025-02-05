package com.sequenceiq.cloudbreak.core.flow2.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;

public class ImageValidationTriggerEvent extends StackEvent {

    public ImageValidationTriggerEvent(
            String selector,
            Long stackId,
            Promise<AcceptResult> accepted) {
        super(selector, stackId, accepted);
    }

    @JsonCreator
    public ImageValidationTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
    }

    public ImageValidationTriggerEvent(String selector, ImageChangeDto imageChangeDto) {
        super(selector, imageChangeDto.getStackId());
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ImageValidationTriggerEvent.class, other);
    }
}
