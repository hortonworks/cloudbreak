package com.sequenceiq.it.cloudbreak.newway.action;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.CloudbreakEntity;

@FunctionalInterface
public interface ActionV2<T extends CloudbreakEntity> {

    T action(TestContext testContext, T entity, CloudbreakClient cloudbreakClient) throws Exception;
}
