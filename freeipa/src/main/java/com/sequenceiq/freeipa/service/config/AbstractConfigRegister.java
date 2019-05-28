package com.sequenceiq.freeipa.service.config;


import javax.inject.Inject;

import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

public abstract class AbstractConfigRegister {

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private StackService stackService;

    protected FreeIpaService getFreeIpaService() {
        return freeIpaService;
    }

    protected Stack getStackWithInstanceMetadata(Long stackId) {
        return stackService.getByIdWithListsInTransaction(stackId);
    }

    public abstract void register(Long stackId);

    public abstract void delete(Stack stack);
}
