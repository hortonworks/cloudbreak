package com.sequenceiq.freeipa.converter.stack;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.dto.StackEvent;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class StackToStackEventConverter {

    public StackEvent convert(Stack source) {
        StackEvent stackEvent = new StackEvent();
        stackEvent.setId(source.getId());
        stackEvent.setName(source.getName());
        stackEvent.setResourceCrn(source.getResourceCrn());
        stackEvent.setEnvironmentCrn(source.getEnvironmentCrn());
        stackEvent.setCloudPlatform(source.getCloudPlatform());
        stackEvent.setRegion(source.getRegion());
        stackEvent.setAvailabilityZone(source.getAvailabilityZone());
        stackEvent.setAccountId(source.getAccountId());
        if (source.getStackStatus() != null) {
            stackEvent.setStatus(source.getStackStatus().getStatus());
        }
        return stackEvent;
    }
}

