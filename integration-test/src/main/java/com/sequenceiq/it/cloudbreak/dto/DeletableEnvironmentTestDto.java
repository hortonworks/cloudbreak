package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public abstract class DeletableEnvironmentTestDto<R, S, T extends CloudbreakTestDto, Z> extends AbstractEnvironmentTestDto<R, S, T>
        implements Purgable<Z, EnvironmentClient> {

    protected DeletableEnvironmentTestDto(String newId) {
        super(newId);
    }

    protected DeletableEnvironmentTestDto(R request, TestContext testContext) {
        super(request, testContext);
    }

    @Override
    public boolean deletable(Z entity) {
        return name(entity).startsWith(getResourcePropertyProvider().prefix());
    }

    protected abstract String name(Z entity);

    @Override
    public Class<EnvironmentClient> client() {
        return EnvironmentClient.class;
    }
}
