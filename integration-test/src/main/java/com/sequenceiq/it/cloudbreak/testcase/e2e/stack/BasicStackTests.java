package com.sequenceiq.it.cloudbreak.testcase.e2e.stack;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

/*
 * This test is used by the pull request builder, when the 'aws_e2e_test' label is applied
 */
public class BasicStackTests extends AbstractE2ETest {

    @Inject
    private StackTestClient stackTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetworkAndFreeIPA(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent AND the stack is scaled up AND the stack is scaled down " +
                    "AND the stack is stopped AND the stack is started",
            then = "the stack should be available AND deletable")
    public void testCreateScaleStopStartCluster(TestContext testContext) {
        String cm = resourcePropertyProvider().getName();
        String cmcluster = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();

        Integer upscaleCount = 4;
        Integer downscaleCount = 3;
        String groupToScale = "worker";

        testContext.given(cm, ClouderaManagerTestDto.class)
                .given(cmcluster, ClusterTestDto.class)
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(cm)
                .given(stack, StackTestDto.class)
                .withCluster(cmcluster)
                .when(stackTestClient.createV4(), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .when(stackTestClient.scalePostV4()
                        .withGroup(groupToScale)
                        .withDesiredCount(upscaleCount), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .then((ctx, stackDto, cloudbreakClient) -> stackTestClient.getV4().action(ctx, stackDto, cloudbreakClient))
                .then(checkWorkerGroupNodeCount(groupToScale, upscaleCount))
                .when(stackTestClient.scalePostV4()
                        .withGroup(groupToScale)
                        .withDesiredCount(downscaleCount), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .then((ctx, stackDto, cloudbreakClient) -> stackTestClient.getV4().action(ctx, stackDto, cloudbreakClient))
                .then(checkWorkerGroupNodeCount(groupToScale, downscaleCount))
                .when(stackTestClient.stopV4(), key(stack))
                .await(STACK_STOPPED)
                .when(stackTestClient.startV4(), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .then((tc, testDto, cc) -> stackTestClient.deleteV4().action(tc, testDto, cc))
                .validate();
    }

    private Assertion<StackTestDto, CloudbreakClient> checkWorkerGroupNodeCount(String scaledGroup, int expectedCount) {
        return (ctx, testDto, cloudbreakClient) -> {
            Integer nodeCount = testDto.getResponse().getInstanceGroups().stream()
                    .filter(group -> scaledGroup.equals(group.getName()))
                    .map(InstanceGroupV4Base::getNodeCount)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No '" + scaledGroup + "' group found in stack."));
            if (expectedCount != nodeCount) {
                String errorMessage = "Group '" + scaledGroup + "' does not have the desired node count. expected: " + expectedCount + " actual: " + nodeCount;
                throw new IllegalArgumentException(errorMessage);
            }
            return testDto;
        };
    }
}
