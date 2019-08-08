package com.sequenceiq.it.cloudbreak.testcase.mock.cluster;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class MockStackCreationTest extends AbstractIntegrationTest {

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid stack request",
            when = "create stack twice",
            then = "getting BadRequestException in the second time because the names are same")
    public void testAttemptToCreateTwoRegularClusterWithTheSameName(TestContext testContext) {
        String badRequest = resourcePropertyProvider().getName();
        testContext.given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .when(stackTestClient.createV4(), RunningParameter.key(badRequest))
                .expect(BadRequestException.class, RunningParameter.key(badRequest))
                .validate();
    }
}
