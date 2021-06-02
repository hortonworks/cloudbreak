package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
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
                .mockCm().users().get().atLeast(1).verify()
                .mockCm().users().put().pathVariable("users", "admin").times(1).verify()
                .validate();
    }
}
