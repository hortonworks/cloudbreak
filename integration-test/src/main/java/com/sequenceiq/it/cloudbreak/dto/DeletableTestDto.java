package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public abstract class DeletableTestDto<R, S, T extends CloudbreakTestDto, Z> extends AbstractCloudbreakTestDto<R, S, T> implements Purgable<Z> {

    protected DeletableTestDto(String newId) {
        super(newId);
    }

    protected DeletableTestDto(R request, TestContext testContext) {
        super(request, testContext);
    }

    @Override
    public boolean deletable(Z entity) {
        return name(entity).startsWith(resourceProperyProvider().prefix());
    }

    protected abstract String name(Z entity);
}
