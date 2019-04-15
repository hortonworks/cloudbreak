package com.sequenceiq.it.cloudbreak.newway.testcase.e2e;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;

public class BasicStackTests extends AbstractE2ETest {

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent AND the stack is stoppend AND the stack is started",
            then = "the stack should be available AND deletable")
    public void testCreateStopAndStartCluster(TestContext testContext) {
        testContext.given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.stopV4())
                .await(STACK_STOPPED)
                .when(stackTestClient.startV4())
                .await(STACK_AVAILABLE)
                .then((tc, testDto, cc) -> stackTestClient.deleteV4().action(tc, testDto, cc))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent AND the stack is scaled",
            then = "the scaled stack should be available")
    public void testCreateAndScaleCluster(TestContext testContext) {
        testContext.given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.scalePostV4()
                        .withGroup("worker")
                        .withDesiredCount(3))
                .await(STACK_AVAILABLE)
                .validate();
    }
}
