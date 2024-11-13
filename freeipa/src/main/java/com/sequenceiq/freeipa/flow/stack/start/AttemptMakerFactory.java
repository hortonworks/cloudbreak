package com.sequenceiq.freeipa.flow.stack.start;

import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.FreeIpaInstanceHealthDetailsService;

@Service
public class AttemptMakerFactory {
    @Inject
    private FreeIpaInstanceHealthDetailsService freeIpaInstanceHealthDetailsService;

    public OneFreeIpaReachableAttempt create(Stack stack, Set<InstanceMetaData> instanceMetaDataSet, int consecutiveSuccess) {
        if (stack == null || instanceMetaDataSet == null || instanceMetaDataSet.isEmpty()) {
            throw new IllegalArgumentException("Stack and instanceMetaDataSet should not been empty");
        }
        return new OneFreeIpaReachableAttempt(freeIpaInstanceHealthDetailsService, stack, instanceMetaDataSet, consecutiveSuccess);
    }
}
