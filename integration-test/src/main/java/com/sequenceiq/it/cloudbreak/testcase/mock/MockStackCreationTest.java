package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;

public class MockStackCreationTest extends AbstractMockTest {

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid stack request",
            when = "create stack twice",
            then = "getting BadRequestException in the second time because the names are same")
    public void testAttemptToCreateTwoRegularClusterWithTheSameName(MockedTestContext testContext) {
        testContext.given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .whenException(stackTestClient.createV4(), BadRequestException.class)
                .validate();
    }
}
