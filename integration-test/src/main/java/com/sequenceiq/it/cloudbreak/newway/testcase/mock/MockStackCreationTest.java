package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

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
                .when(stackTestClient.createV4(), key(badRequest))
                .expect(BadRequestException.class, key(badRequest))
                .validate();
    }
}
