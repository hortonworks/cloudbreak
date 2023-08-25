package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.io.IOException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.MinionIpAddressesResponse;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager.AbstractClouderaManagerTest;

public class DistroXClusterFaultTolerantUpscaleTest extends AbstractClouderaManagerTest {

    private static final int SUCCESSFUL_UPSCALE_NODE_COUNT = 10;

    private static final int FAILING_NODE_COUNT = 2;

    private static final String UPSCALED_INSTANCE_GROUP_NAME = "worker";

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
            when = "an upscale is called, but some of the new nodes are unreachable",
            then = "the cluster should be AVAILABLE in a partially upscaled state")
    public void testUpscaleWithFewNodesBeingUnreachable(MockedTestContext testContext, ITestContext testNgContext) {
        String stack = resourcePropertyProvider().getName();
        createDatalake(testContext);

        MinionIpAddressesResponse minionIpAddressesResponse = getMockResponseForIpAddrsSaltCall(stack);
        Set<String> saltBodyFilters = getSaltBodyFilters(stack);

        DistroXTestDto currentContext = setUpContextForUpscale(testContext, stack);
        currentContext
                .mockSalt().run().post().bodyContains(saltBodyFilters, 100)
                .thenReturn(minionIpAddressesResponse, null, 200, 0, null)
                .when(distroXClient.scale(UPSCALED_INSTANCE_GROUP_NAME, SUCCESSFUL_UPSCALE_NODE_COUNT))
                .awaitForFlow()
                .await(STACK_AVAILABLE, key(stack).withPollingInterval(POLLING_INTERVAL))
                .then((tc, testDto, client) -> verifyHealthyNodeCountAfterUpscale(testDto));

        currentContext.validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DistroX cluster",
            when = "an upscale is called, but CM's request, waiting for new hosts, times out",
            then = "the cluster should be AVAILABLE in a partially upscaled state")
    public void testUpscaleWithWaitingForHostsTimingOut(MockedTestContext testContext, ITestContext testNgContext) {
        String stack = resourcePropertyProvider().getName();
        createDatalake(testContext);

        ApiHostList apiHostList = getMockApiHostList();

        DistroXTestDto currentContext = setUpContextForUpscale(testContext, stack);
        currentContext
                .mockCm().hosts().get().thenReturn(apiHostList, null, 200, 0, null)
                .when(distroXClient.scale(UPSCALED_INSTANCE_GROUP_NAME, SUCCESSFUL_UPSCALE_NODE_COUNT))
                .awaitForFlow(RunningParameter.waitForFlowSuccess())
                .await(STACK_AVAILABLE, key(stack).withPollingInterval(POLLING_INTERVAL))
                .then((tc, testDto, client) -> verifyHealthyNodeCountAfterUpscale(testDto));

        currentContext.validate();
    }

    private DistroXTestDto setUpContextForUpscale(MockedTestContext testContext, String stack) {
        return testContext
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
                .awaitForFlow()
                .await(STACK_AVAILABLE, key(stack).withIgnoredStatues(Set.of(Status.UNREACHABLE)));
    }

    private MinionIpAddressesResponse getMockResponseForIpAddrsSaltCall(String stack) {
        int successfulNodeCount = SUCCESSFUL_UPSCALE_NODE_COUNT - FAILING_NODE_COUNT;
        Map<String, JsonNode> ipAddressesForMinions = new HashMap<>();
        try {
            for (int i = 3; i < successfulNodeCount; i++) {
                ipAddressesForMinions.put(stack + "-worker" + i + ".ipatest.local", JsonUtil.readTree("[\"192.3.0." + i + "\"]"));
            }
            for (int i = 0; i < FAILING_NODE_COUNT; i++) {
                ipAddressesForMinions.put(stack + "-worker" + (i + successfulNodeCount) + ".ipatest.local", JsonUtil.readTree("[\"\"]"));
            }
        } catch (IOException e) {
            throw new TestFailException("The specified IP addresses could not be deserialized. IOException: " + e.getMessage());
        }

        List<Map<String, JsonNode>> result = new ArrayList<>();
        result.add(ipAddressesForMinions);
        MinionIpAddressesResponse minionIpAddressesResponse = new MinionIpAddressesResponse();
        minionIpAddressesResponse.setResult(result);
        return minionIpAddressesResponse;
    }

    private Set<String> getSaltBodyFilters(String stackName) {
        Set<String> saltBodyfilters = new HashSet<>();
        saltBodyfilters.add("fun=network.ipaddrs");
        for (int i = 3; i < SUCCESSFUL_UPSCALE_NODE_COUNT; i++) {
            saltBodyfilters.add(stackName + "-worker" + i + ".ipatest.local");
        }
        return saltBodyfilters;
    }

    private ApiHostList getMockApiHostList() {
        ApiHostList apiHostList = new ApiHostList();
        ApiHost master = new ApiHost();
        master.setIpAddress("192.0.0.0");
        master.setHostname("master0.ipatest.local");
        master.setLastHeartbeat(ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(10).format(DateTimeFormatter.ISO_INSTANT));
        apiHostList.addItemsItem(master);
        ApiHost gateway = new ApiHost();
        gateway.setIpAddress("192.2.0.0");
        gateway.setHostname("gateway0.ipatest.local");
        gateway.setLastHeartbeat(ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(10).format(DateTimeFormatter.ISO_INSTANT));
        apiHostList.addItemsItem(gateway);

        for (int i = 0; i < SUCCESSFUL_UPSCALE_NODE_COUNT; i++) {
            ApiHost temp = new ApiHost();
            temp.setIpAddress("192.3.0." + i);
            temp.setHostname("worker" + i + ".ipatest.local");
            if (i < SUCCESSFUL_UPSCALE_NODE_COUNT - FAILING_NODE_COUNT) {
                temp.setLastHeartbeat(ZonedDateTime.now(ZoneOffset.UTC).plusMinutes(10).format(DateTimeFormatter.ISO_INSTANT));
            }
            apiHostList.addItemsItem(temp);
        }

        return apiHostList;
    }

    private DistroXTestDto verifyHealthyNodeCountAfterUpscale(DistroXTestDto testDto) {
        List<InstanceGroupV4Response> instanceGroups = testDto.getResponse().getInstanceGroups();
        Optional<InstanceGroupV4Response> upscaledInstanceGroup = instanceGroups.stream().filter(ig ->
                UPSCALED_INSTANCE_GROUP_NAME.equals(ig.getName())).findFirst();
        if (upscaledInstanceGroup.isEmpty()) {
            throw new TestFailException("Upscaled instance group " + UPSCALED_INSTANCE_GROUP_NAME + " could not be found in the response.");
        }

        long healthyNodeCount = upscaledInstanceGroup.get().getMetadata().stream()
                .filter(md -> md.getInstanceStatus() == InstanceStatus.SERVICES_HEALTHY).count();
        if (healthyNodeCount != SUCCESSFUL_UPSCALE_NODE_COUNT - FAILING_NODE_COUNT) {
            throw new TestFailException("The number of healthy nodes in instance group " + UPSCALED_INSTANCE_GROUP_NAME + " should be equal to: " +
                    (SUCCESSFUL_UPSCALE_NODE_COUNT - FAILING_NODE_COUNT) + " but is actually: " + healthyNodeCount);
        }

        return testDto;
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }
}
