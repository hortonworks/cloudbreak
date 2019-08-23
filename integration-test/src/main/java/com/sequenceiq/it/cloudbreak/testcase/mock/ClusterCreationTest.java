package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.action.stack.StackTestAction;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class ClusterCreationTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((MockedTestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a stack without network and placement",
            when = "post is called",
            then = "should bad request exception")
    public void createClusterWithoutNetworkAndPlacementShouldBadRequestException(MockedTestContext testContext) {
        testContext
                .given(StackTestDto.class)
                .withEmptyNetwork()
                .withEmptyPlacement()
                .when(StackTestAction::create, key("error"))
                .expect(BadRequestException.class, key("error")
                        .withExpectedMessage("We cannot determine the subnet from environment. Please add 'network' or 'placement' to the request"))
                .validate();
    }
}
