package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import javax.inject.Inject;

import org.springframework.http.HttpMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.mock.model.ClouderaManagerMock;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class ClouderaManagerSetupTest extends AbstractIntegrationTest {

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a working environment",
            when = "a stack is created",
            then = "ClouderaManager user endpoints should be invoked with the proper requests")
    public void verifyCallsAgainstCmUserCreation(TestContext testContext) {
        String envName = resourcePropertyProvider().getName();

        testContext
                .given(EnvironmentTestDto.class)
                .withName(envName)
                .when(environmentTestClient.createV4())
                .given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.GET, ClouderaManagerMock.USERS).exactTimes(1))
                .then(MockVerification.verify(HttpMethod.PUT, "/api/v31/users/admin").exactTimes(1))
                .validate();
    }
}
