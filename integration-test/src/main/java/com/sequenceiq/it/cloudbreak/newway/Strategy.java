package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

public interface Strategy {
    void doAction(IntegrationTestContext integrationTestContext, Entity entity) throws Exception;
}
