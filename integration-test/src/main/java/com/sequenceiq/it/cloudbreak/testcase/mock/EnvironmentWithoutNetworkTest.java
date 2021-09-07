package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;

public class EnvironmentWithoutNetworkTest extends AbstractMockTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "an environment creation request sent without network",
            then = "the request should return with bad request exception")
    public void testEnvironmentPostRequestWhenNetworkIsEmpty(MockedTestContext testContext) {
        testContext.given(EnvironmentTestDto.class)
                .withCreateFreeIpa(Boolean.FALSE)
                .withoutNetwork()
                .when(environmentTestClient.create(), RunningParameter.key("post"))
                .expect(BadRequestException.class, RunningParameter.expectedMessage("Environment network cannot be null").withKey("post"))
                .validate();
    }
}
