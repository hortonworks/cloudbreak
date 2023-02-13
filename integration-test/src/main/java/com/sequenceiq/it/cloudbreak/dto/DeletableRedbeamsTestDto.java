package com.sequenceiq.it.cloudbreak.dto;

import com.sequenceiq.it.cloudbreak.context.Purgable;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;

public abstract class DeletableRedbeamsTestDto<R, S, T extends CloudbreakTestDto, Z> extends AbstractRedbeamsTestDto<R, S, T>
        implements Purgable<Z, RedbeamsClient> {

    protected DeletableRedbeamsTestDto(R request, TestContext testContext) {
        super(request, testContext);
    }

    @Override
    public boolean deletable(Z entity) {
        return name(entity).startsWith(getResourcePropertyProvider().prefix(getCloudPlatform()));
    }

    protected abstract String name(Z entity);

    @Override
    public Class<RedbeamsClient> client() {
        return RedbeamsClient.class;
    }
}
