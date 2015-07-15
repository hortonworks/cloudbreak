package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Component
public class AzureCreateResourceStatusCheckerTask extends AzureResourceStatusCheckerTask {

    @Inject
    private StackRepository stackRepository;

    public boolean exitPolling(AzureResourcePollerObject t) {
        try {
            Stack stack = stackRepository.findByIdLazy(t.getStack().getId());
            if (stack == null || stack.isDeleteInProgress()) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            return true;
        }
    }

}
