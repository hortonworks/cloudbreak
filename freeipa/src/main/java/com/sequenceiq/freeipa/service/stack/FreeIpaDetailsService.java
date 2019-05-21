package com.sequenceiq.freeipa.service.stack;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.FreeIpaService;

@Service
public class FreeIpaDetailsService {

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    public FreeIPADetails details(String accountId, String environment, String name) {
        Stack stack = stackService.getByAccountIdEnvironmentAndName(accountId, environment, name);
        FreeIpa freeIpa = freeIpaService.findByStack(stack);

        FreeIPADetails details = new FreeIPADetails();
        details.setStack(stack);
        details.setFreeIpa(freeIpa);
        return details;
    }
}
