package com.sequenceiq.freeipa.flow.stack.start;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.FreeIpaInstanceHealthDetailsService;

@Service
public class AttemptMakerFactory {
    private FreeIpaInstanceHealthDetailsService freeIpaInstanceHealthDetailsService;

    public AttemptMakerFactory(FreeIpaInstanceHealthDetailsService freeIpaInstanceHealthDetailsService) {
        this.freeIpaInstanceHealthDetailsService = freeIpaInstanceHealthDetailsService;
    }

    public OneFreeIpaReachableAttempt create(Stack stack, Set<InstanceMetaData> instanceMetaDataSet) {
        if (stack == null || instanceMetaDataSet == null || instanceMetaDataSet.isEmpty()) {
            throw new IllegalArgumentException("Stack and instanceMetaDataSet should not been empty");
        }
        return new OneFreeIpaReachableAttempt(freeIpaInstanceHealthDetailsService, stack, instanceMetaDataSet);
    }
}
