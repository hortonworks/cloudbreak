package com.sequenceiq.it.cloudbreak.context;

import javax.inject.Inject;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.mock.ExecuteQueryToMockInfrastructure;

@Prototype
public class MockedTestContext extends TestContext {

    @Inject
    private ExecuteQueryToMockInfrastructure executeQueryToMockInfrastructure;

    public ExecuteQueryToMockInfrastructure getExecuteQueryToMockInfrastructure() {
        return executeQueryToMockInfrastructure;
    }
}
