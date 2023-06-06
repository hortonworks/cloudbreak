package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.pollingInterval;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager.AbstractClouderaManagerTest;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;

public class DistroXClusterDownscaleWithDeletedNodesTest extends AbstractClouderaManagerTest {

    private static final String IMAGE_CATALOG_ID = "f6e778fc-7f17-4535-9021-515351df3691";

    private static final String CM_FOR_DISTRO_X = "cm4dstrx";

    private static final String CLUSTER_KEY = "cmdistrox";

    private static final String DIX_IMG_KEY = "dixImg";

    private static final String DIX_NET_KEY = "dixNet";

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private DistroxUtil distroxUtil;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DistroX cluster",
            when = "scale to 10 and terminate 2 instances, then scale down to 8",
            then = "the cluster should be available and should have 8 worker nodes")
    public void testScaleDownWithDeletedInstances(MockedTestContext testContext, ITestContext testNgContext) {
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
                .await(STACK_AVAILABLE, key(stack))
                .when(distroXClient.scale(HostGroupType.WORKER.getName(), 10))
                .await(STACK_AVAILABLE, key(stack).withPollingInterval(Duration.ofSeconds(2)))
                .then((tc, testDto, client) -> {
                    List<String> workerInstanceIds = distroxUtil.getInstanceIds(testDto, client, HostGroupType.WORKER.getName());
                    List<String> twoWorkerInstanceIds = workerInstanceIds.stream().limit(2).toList();
                    twoWorkerInstanceIds.forEach(instanceId ->
                            getExecuteQueryToMockInfrastructure().call(testDto.getCrn() + "/spi/" + instanceId + "/terminate", w -> w));
                    return testDto;
                })
                .await(STACK_NODE_FAILURE, pollingInterval(Duration.ofSeconds(2)))
                .when(distroXClient.scale(HostGroupType.WORKER.getName(), 8))
                .await(STACK_AVAILABLE, key(stack).withPollingInterval(Duration.ofSeconds(2)))
                .then((tc, testDto, client) -> {
                    Set<InstanceMetaDataV4Response> workerInstances = testDto.getResponse().getInstanceGroups().stream()
                            .filter(instanceGroupV4Response -> HostGroupType.WORKER.getName().equals(instanceGroupV4Response.getName()))
                            .findFirst().get().getMetadata();
                    if (workerInstances.size() != 8) {
                        throw new TestFailException("Worker instances size should be 8 after downscale");
                    }
                    return testDto;
                });

        currentContext
                .validate();
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }
}