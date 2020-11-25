package com.sequenceiq.freeipa.flow.stack.start;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.FreeIpaHealthDetailsService;

@Service
public class AttemptMakerFactory {
    private FreeIpaHealthDetailsService freeIpaHealthDetailsService;

    public AttemptMakerFactory(FreeIpaHealthDetailsService freeIpaHealthDetailsService) {
        this.freeIpaHealthDetailsService = freeIpaHealthDetailsService;
    }

    public OneFreeIpaReachableAttempt create(Stack stack, Set<InstanceMetaData> instanceMetaDataSet) {
        if (stack == null || instanceMetaDataSet == null || instanceMetaDataSet.isEmpty()) {
            throw new IllegalArgumentException("Stack and instanceMetaDataSet should not been empty");
        }
        return new OneFreeIpaReachableAttempt(freeIpaHealthDetailsService, stack, instanceMetaDataSet);
    }
}
