package com.sequenceiq.it.cloudbreak.testcase.e2e.aws;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerRepositoryTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerProductTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class AwsBasicStackTests extends AbstractE2ETest {

    private static final String REPOSITORY_VERSION = "7.x.0";

    private static final String REPOSITORY_URL = "http://cloudera-build-us-west-1.vpc.cloudera.com/s3/build/1072240/cm7/7.x.0/redhat7/yum/";

    private static final String REPOSITORY_GPG = "";

    private static final String PRODUCT_VERSION = "6.0.99-1.cdh6.0.99.p0.181";

    private static final String PRODUCT_PARCEL = "http://cloudera-build-us-west-1.vpc.cloudera.com/s3/build/1071671/cdh/6.x/parcels/";

    private static final String PRODUCT_NAME = "CDH";

    @Inject
    private StackTestClient stackTestClient;

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid stack create request is sent AND the stack is stopped AND the stack is started",
            then = "the stack should be available AND deletable")
    public void testCreateStopAndStartCluster(TestContext testContext) {
        String cm = resourcePropertyProvider().getName();
        String cmcluster = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();

        testContext.given(cm, ClouderaManagerTestDto.class)
                .withClouderaManagerRepository(new ClouderaManagerRepositoryTestDto(testContext)
                        .withVersion(REPOSITORY_VERSION).withBaseUrl(REPOSITORY_URL))
                .withClouderaManagerProduct(new ClouderaManagerProductTestDto(testContext)
                        .withName(PRODUCT_NAME).withParcel(PRODUCT_PARCEL).withVersion(PRODUCT_VERSION))
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
                        .withVersion(REPOSITORY_VERSION).withBaseUrl(REPOSITORY_URL).withGpgKeyUrl(REPOSITORY_GPG))
                .withClouderaManagerProduct(new ClouderaManagerProductTestDto(testContext)
                        .withName(PRODUCT_NAME).withParcel(PRODUCT_PARCEL).withVersion(PRODUCT_VERSION))
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

    private Assertion<StackTestDto> checkWorgerGroupNodeCount(String scaledGroup, int expectedCount) {
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
