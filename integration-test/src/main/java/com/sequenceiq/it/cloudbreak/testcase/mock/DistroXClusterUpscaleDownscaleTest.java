package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertEquals;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceTemplateTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager.AbstractClouderaManagerTest;

public class DistroXClusterUpscaleDownscaleTest extends AbstractClouderaManagerTest {

    private static final int CLUSTER_NODE_COUNT_MAX = 1000;

    private static final int CLUSTER_NODE_COUNT_MIN = 2;

    private static final int WORKER_NODE_COUNT_MAX = CLUSTER_NODE_COUNT_MAX - CLUSTER_NODE_COUNT_MIN;

    private static final int WORKER_NODE_COUNT_MIN = 3;

    private static final int UPPER_NODE_COUNT = 10;

    private static final int LOWER_NODE_COUNT = 5;

    private static final String IMAGE_CATALOG_ID = "f6e778fc-7f17-4535-9021-515351df3691";

    private static final String CM_FOR_DISTRO_X = "cm4dstrx";

    private static final String CLUSTER_KEY = "cmdistrox";

    private static final String DIX_IMG_KEY = "dixImg";

    private static final String DIX_NET_KEY = "dixNet";

    private static final Duration POLLING_INTERVAL = Duration.of(3000, ChronoUnit.MILLIS);

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DistroX cluster",
            when = "a scale, start stop called many times",
            then = "the cluster should be available")
    public void testScaleDownAndUp(MockedTestContext testContext, ITestContext testNgContext) {
        DistroXStartStopTestParameters params = new DistroXStartStopTestParameters(testNgContext.getCurrentXmlTest().getAllParameters());
        String stack = resourcePropertyProvider().getName();
        createDatalake(testContext);
        DistroXTestDto currentContext = testContext
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog()
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given(stack, DistroXTestDto.class)
                .withCluster(CLUSTER_KEY)
                .withName(stack)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.create(), key(stack))
                .await(STACK_AVAILABLE, key(stack));
        for (int i = 0; i < 3; i++) {
            currentContext = currentContext
                    .when(distroXClient.scale(params.getHostgroup(), UPPER_NODE_COUNT))
                    .await(STACK_AVAILABLE, key(stack).withPollingInterval(POLLING_INTERVAL));

            currentContext = currentContext
                    .when(distroXClient.scale(params.getHostgroup(), LOWER_NODE_COUNT))
                    .await(STACK_AVAILABLE, key(stack).withPollingInterval(POLLING_INTERVAL));
        }

        currentContext
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DistroX cluster",
            when = "a scale is called where multiple instances fail on provider side",
            then = "the cluster should become available")
    public void testScaleUpWithNodeFailure(MockedTestContext testContext, ITestContext testNgContext) {
        DistroXStartStopTestParameters params = new DistroXStartStopTestParameters(testNgContext.getCurrentXmlTest().getAllParameters());
        String stack = resourcePropertyProvider().getName();
        createDatalake(testContext);
        testContext
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog()
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given(stack, DistroXTestDto.class)
                .withCluster(CLUSTER_KEY)
                .withName(stack)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.create(), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .setNewInstanceFailure(5)
                .when(distroXClient.scale(params.getHostgroup(), UPPER_NODE_COUNT))
                .await(STACK_AVAILABLE, key(stack).withPollingInterval(POLLING_INTERVAL))
                .then(assertInstanceCount(params.getHostgroup(), UPPER_NODE_COUNT - 5))
                .setNewInstanceFailure(3)
                .when(distroXClient.scale(params.getHostgroup(), UPPER_NODE_COUNT))
                .await(STACK_AVAILABLE, key(stack).withPollingInterval(POLLING_INTERVAL))
                .then(assertInstanceCount(params.getHostgroup(), UPPER_NODE_COUNT - 3))
                .setNewInstanceFailure(0)
                .when(distroXClient.scale(params.getHostgroup(), UPPER_NODE_COUNT))
                .await(STACK_AVAILABLE, key(stack).withPollingInterval(POLLING_INTERVAL))
                .then(assertInstanceCount(params.getHostgroup(), UPPER_NODE_COUNT))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DistroX cluster",
            when = "a scale is called where multiple cm agens fail",
            then = "the cluster should become available and additional scale should work")
    public void testScaleUpWithCmAgentFailure(MockedTestContext testContext, ITestContext testNgContext) {
        DistroXStartStopTestParameters params = new DistroXStartStopTestParameters(testNgContext.getCurrentXmlTest().getAllParameters());
        String stack = resourcePropertyProvider().getName();
        createDatalake(testContext);
        testContext
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog()
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given(stack, DistroXTestDto.class)
                .withCluster(CLUSTER_KEY)
                .withName(stack)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.create(), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .scheduleFailureOnCommand("CM_READ_HOSTS", "worker", 10, 5)
                .when(distroXClient.scale(params.getHostgroup(), UPPER_NODE_COUNT))
                .await(STACK_AVAILABLE, key(stack))
                .scheduleFailureOnCommand("CM_READ_HOSTS", "worker", 0, 0)
                .when(distroXClient.scale(params.getHostgroup(), 3))
                .await(STACK_AVAILABLE, key(stack))
                .when(distroXClient.scale(params.getHostgroup(), UPPER_NODE_COUNT))
                .await(STACK_AVAILABLE, key(stack))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DistroX cluster with 300 instances in worker group with 5 volumes per instance",
            when = "up- and downscale is called 3 times",
            then = "the cluster should be available")
    public void testScaleDownAndUpWithLargeNodes(MockedTestContext testContext, ITestContext testNgContext) {
        scalingTestWithManyNodes(testContext, 300, 150, 125, 3);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DistroX cluster with 398 instances in worker group",
            when = "up- and downscale is called 30 times",
            then = "the cluster should be available")
    public void testScaleDownAndUpManyTimes(MockedTestContext testContext, ITestContext testNgContext) {
        scalingTestWithManyNodes(testContext, WORKER_NODE_COUNT_MAX, WORKER_NODE_COUNT_MAX, WORKER_NODE_COUNT_MIN, 30);
    }

    private void scalingTestWithManyNodes(MockedTestContext testContext, int workerInitialNodeCount, int workerNodeCountAfterUpscale,
            int workerNodeCountAfterDownscale, int scalingCycles) {
        String stack = resourcePropertyProvider().getName();
        createDatalake(testContext);
        DistroXTestDto currentContext = createDistroxDto(testContext, stack, workerInitialNodeCount)
                .when(distroXClient.create(), key(stack))
                .await(STACK_AVAILABLE, key(stack));
        for (int i = 0; i < scalingCycles; i++) {
            currentContext = currentContext
                    .when(distroXClient.scale(HostGroupType.WORKER.getName(), workerNodeCountAfterDownscale))
                    .await(STACK_AVAILABLE, key(stack).withPollingInterval(POLLING_INTERVAL));

            currentContext = currentContext
                    .when(distroXClient.scale(HostGroupType.WORKER.getName(), workerNodeCountAfterUpscale))
                    .await(STACK_AVAILABLE, key(stack).withPollingInterval(POLLING_INTERVAL));
        }

        currentContext
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running datalake",
            when = "a DostroX is created with 401 nodes or scaled to 401 nodes",
            then = "the creation or scale should fail")
    public void testNodeCountLimit(MockedTestContext testContext, ITestContext testNgContext) {
        String stack = resourcePropertyProvider().getName();
        createDatalake(testContext);
        createDistroxDto(testContext, stack, WORKER_NODE_COUNT_MAX + 1)
                .whenException(distroXClient.create(), BadRequestException.class, key(stack)
                        .withExpectedMessage("The maximum count of nodes for this cluster cannot be higher than " + CLUSTER_NODE_COUNT_MAX))
                .given("dx-1000-ig-worker", DistroXInstanceGroupTestDto.class)
                .withNodeCount(350)
                .given(stack, DistroXTestDto.class)
                .when(distroXClient.create(), key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .whenException(distroXClient.scale(HostGroupType.WORKER.getName(), CLUSTER_NODE_COUNT_MAX + 1), BadRequestException.class, key(stack)
                        .withExpectedMessage("The maximum count of nodes for this cluster cannot be higher than " + CLUSTER_NODE_COUNT_MAX))
                .validate();
    }

    private DistroXTestDtoBase<DistroXTestDto> createDistroxDto(MockedTestContext testContext, String stack, int workerNodeCount) {
        return testContext
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog()
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given("dx-5-volume-ig-template", DistroXInstanceTemplateTestDto.class)
                .withAttachedVolumes(5)
                .given("dx-1000-ig-worker", DistroXInstanceGroupTestDto.class)
                .withHostGroup(HostGroupType.WORKER)
                .withTemplate("dx-5-volume-ig-template")
                .withNodeCount(workerNodeCount)
                .given("dx-ig-master", DistroXInstanceGroupTestDto.class)
                .withHostGroup(HostGroupType.MASTER)
                .given("dx-ig-gw", DistroXInstanceGroupTestDto.class)
                .withHostGroup(HostGroupType.GATEWAY)
                .given("dx-ig-compute", DistroXInstanceGroupTestDto.class)
                .withHostGroup(HostGroupType.COMPUTE)
                .given(stack, DistroXTestDto.class)
                .withInstanceGroups("dx-1000-ig-worker", "dx-ig-master", "dx-ig-gw", "dx-ig-compute")
                .withCluster(CLUSTER_KEY)
                .withName(stack)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY);
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }

    private static Assertion<DistroXTestDto, CloudbreakClient> assertInstanceCount(String hostGroup, int instanceCount) {
        return (tc, testDto, client) -> {
            assertEquals(client.getDefaultClient(tc).distroXV1Endpoint().getByName(testDto.getName(), new HashSet<>())
                    .getInstanceGroups()
                    .stream()
                    .filter(instanceGroup -> hostGroup.equals(instanceGroup.getName()))
                    .findFirst()
                    .get()
                    .getMetadata().size(), instanceCount);
            return testDto;
        };
    }
}

