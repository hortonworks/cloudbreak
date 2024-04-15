package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.RemoteEnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.RemoteEnvironmentTestDto;

public class RemoteEnvironmentTest extends AbstractMockTest {

    @Inject
    private RemoteEnvironmentTestClient remoteEnvironmentTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a remote environment service",
            when = "list the remote envs",
            then = "there should be http200 code call")
    public void testRemoteEnvironmentService(MockedTestContext testContext) {
        testContext
                .given(RemoteEnvironmentTestDto.class)
                .when(remoteEnvironmentTestClient.list())
                .validate();
    }
}
