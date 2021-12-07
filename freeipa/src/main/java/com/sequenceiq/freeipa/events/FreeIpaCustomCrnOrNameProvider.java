package com.sequenceiq.freeipa.events;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.repository.AccountAwareResource;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component("freeipaCustomCrnOrNameProvider")
public class FreeIpaCustomCrnOrNameProvider extends AbstractCustomCrnOrNameProvider {

    @Inject
    private StackService stackService;

    @Override
    protected List<? extends AccountAwareResource> getResource(String environmentCrn, String accountId) {
        return stackService.findMultipleByEnvironmentCrnAndAccountIdEvenIfTerminated(environmentCrn, accountId);
    }
}
