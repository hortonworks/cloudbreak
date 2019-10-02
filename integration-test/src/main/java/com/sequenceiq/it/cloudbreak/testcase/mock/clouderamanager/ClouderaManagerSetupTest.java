package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;

import javax.inject.Inject;

import org.springframework.http.HttpMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.mock.model.ClouderaManagerMock;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class ClouderaManagerSetupTest extends AbstractIntegrationTest {

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a working environment",
            when = "a stack is created",
            then = "ClouderaManager user endpoints should be invoked with the proper requests")
    public void verifyCallsAgainstCmUserCreation(TestContext testContext) {
        testContext
                .given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.GET, ClouderaManagerMock.USERS).exactTimes(1))
                .then(MockVerification.verify(HttpMethod.PUT, "/api/v31/users/admin").exactTimes(1))
                .validate();
    }
}
