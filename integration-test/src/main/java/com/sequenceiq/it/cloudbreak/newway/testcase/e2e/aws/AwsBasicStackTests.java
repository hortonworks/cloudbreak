package com.sequenceiq.it.cloudbreak.newway.testcase.e2e.aws;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.ClouderaManagerProductTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ClouderaManagerRepositoryTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.e2e.AbstractE2ETest;

public class AwsBasicStackTests extends AbstractE2ETest {

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent AND the stack is stoppend AND the stack is started",
            then = "the stack should be available AND deletable")
    public void testCreateStopAndStartCluster(TestContext testContext) {
        String cm = resourcePropertyProvider().getName();
        String cmcluster = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();

        testContext.given(cm, ClouderaManagerTestDto.class)
                .withClouderaManagerRepository(new ClouderaManagerRepositoryTestDto(testContext)
                        .withVersion("7.x.0")
                        .withBaseUrl("http://cloudera-build-us-west-1.vpc.cloudera.com/s3/build/1048788/cm7/7.x.0/redhat7/yum/"))
                .withClouderaManagerProduct(new ClouderaManagerProductTestDto(testContext)
                        .withName("CDH")
                        .withParcel("http://cloudera-build-3-us-west-2.vpc.cloudera.com/s3/build/1048752/cdh/6.x/parcels/")
                        .withVersion("6.0.99-1.cdh6.0.99.p0.134"))
                .given(cmcluster, ClusterTestDto.class)
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(cm)
                .given(stack, StackTestDto.class)
                .withCluster(cmcluster)
                .when(stackTestClient.createV4(), key(stack))
                .await(STACK_AVAILABLE)
                .when(stackTestClient.stopV4(), key(stack))
                .await(STACK_STOPPED)
                .when(stackTestClient.startV4(), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .then((tc, testDto, cc) -> stackTestClient.deleteV4().action(tc, testDto, cc))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent AND the stack is scaled",
            then = "the scaled stack should be available")
    public void testCreateAndScaleCluster(TestContext testContext) {
        String cm = resourcePropertyProvider().getName();
        String cmcluster = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();

        Integer upscaleCount = 4;
        Integer downscaleCount = 3;
        String groupToScale = "worker";
        testContext.given(cm, ClouderaManagerTestDto.class)
                .withClouderaManagerRepository(new ClouderaManagerRepositoryTestDto(testContext)
                        .withVersion("7.x.0")
                        .withBaseUrl("http://cloudera-build-us-west-1.vpc.cloudera.com/s3/build/1048788/cm7/7.x.0/redhat7/yum/"))
                .withClouderaManagerProduct(new ClouderaManagerProductTestDto(testContext)
                        .withName("CDH")
                        .withParcel("http://cloudera-build-3-us-west-2.vpc.cloudera.com/s3/build/1048752/cdh/6.x/parcels/")
                        .withVersion("6.0.99-1.cdh6.0.99.p0.134"))
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
                .then(checkWorgerGroupNodeCount(groupToScale, upscaleCount))
                .when(stackTestClient.scalePostV4()
                        .withGroup(groupToScale)
                        .withDesiredCount(downscaleCount), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .then((ctx, stackDto, cloudbreakClient) -> stackTestClient.getV4().action(ctx, stackDto, cloudbreakClient))
                .then(checkWorgerGroupNodeCount(groupToScale, downscaleCount))
                .validate();
    }

    private AssertionV2<StackTestDto> checkWorgerGroupNodeCount(String scaledGroup, int expectedCount) {
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
