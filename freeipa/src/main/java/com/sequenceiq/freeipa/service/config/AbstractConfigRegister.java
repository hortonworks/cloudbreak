package com.sequenceiq.freeipa.service.config;


import javax.inject.Inject;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
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

    protected String getEnvironmentCrnByStackId(Long stackId) {
        return stackService.getEnvironmentCrnByStackId(stackId);
    }

    public abstract void register(Long stackId);

    protected InstanceMetaData getMasterInstance(Stack stack) {
        InstanceGroup masterGroup =
                stack.getInstanceGroups().stream().filter(instanceGroup -> InstanceGroupType.MASTER == instanceGroup.getInstanceGroupType()).findFirst().get();
        return masterGroup.getNotDeletedInstanceMetaDataSet().stream().findFirst().get();
    }

    public abstract void delete(Stack stack);
}
