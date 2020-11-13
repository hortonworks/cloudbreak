package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.mock.CheckCount;
import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.endpoint.ClouderaManagerEndpoints;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.mock.AbstractMockTest;

public class ClouderaManagerSetupTest extends AbstractMockTest {

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a working environment",
            when = "a stack is created",
            then = "ClouderaManager user endpoints should be invoked with the proper requests")
    public void verifyCallsAgainstCmUserCreation(MockedTestContext testContext) {
        testContext
                .given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .given(HttpMock.class).whenRequested(ClouderaManagerEndpoints.Users.class).get().verify(CheckCount.times(1))
//                .given(HttpMock.class).whenRequested(ClouderaManagerEndpoints.Users.class).get().verify(CheckCount.times(1))
//                .then(MockVerification.verify(HttpMethod.GET, ClouderaManagerMock.USERS).exactTimes(1))
//                .then(MockVerification.verify(HttpMethod.PUT, "/api/v31/users/admin").exactTimes(1))
                .validate();
    }
}
