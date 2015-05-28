package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Component
public class AzureCreateResourceStatusCheckerTask extends AzureResourceStatusCheckerTask {

    @Autowired
    private StackRepository stackRepository;

    public boolean exitPolling(AzureResourcePollerObject t) {
        try {
            Stack stack = stackRepository.findById(t.getStack().getId());
            if (stack == null || stack.isDeleteInProgress()) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            return true;
        }
    }

}
