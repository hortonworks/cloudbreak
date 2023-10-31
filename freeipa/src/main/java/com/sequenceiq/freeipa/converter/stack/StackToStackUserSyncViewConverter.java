package com.sequenceiq.freeipa.converter.stack;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;

@Component
public class StackToStackUserSyncViewConverter {
    public StackUserSyncView convert(Stack stack) {
        return new StackUserSyncView(stack.getId(), stack.getResourceCrn(), stack.getName(), stack.getEnvironmentCrn(), stack.getAccountId(),
                stack.getCloudPlatform(), mapStatus(stack));
    }

    private Status mapStatus(Stack stack) {
        return Optional.ofNullable(stack.getStackStatus())
                .map(StackStatus::getStatus)
                .orElse(null);
    }
}
