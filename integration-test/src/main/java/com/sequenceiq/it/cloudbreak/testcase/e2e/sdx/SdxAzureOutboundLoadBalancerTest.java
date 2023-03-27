package com.sequenceiq.it.cloudbreak.testcase.e2e.sdx;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.loadbalancer.LoadBalancerResponse;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams.EnvironmentNetworkAzureParamsBuilder;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class SdxAzureOutboundLoadBalancerTest extends PreconditionSdxE2ETest {
    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createResourceGroup(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running Cloudbreak, and an environment in available state",
            when = "SDX cluster is created",
            then = "the cluster should be up and running with an outbound load balancer created"
    )
    public void testSDXWithNewNetworkShouldCreateOutboundLoadBalancer(TestContext testContext) {
        String sdx = resourcePropertyProvider().getName();
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NONE);

        createEnvironmentWithNewNetwork(testContext);

        testContext.given(sdx, SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withClusterShape(SdxClusterShape.CUSTOM)
                .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal(), key(sdx))
                .await(SdxClusterStatusResponse.RUNNING, key(sdx))
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    List<LoadBalancerResponse> loadBalancers = sdxUtil.getLoadBalancers(testDto, client);
                    if (loadBalancers.size() != 2) {
                        throw new TestFailException(String.format("The number of loa balancers created is %s instead of 2", loadBalancers.size()));
                    }
                    if (loadBalancers.stream().noneMatch(lb -> lb.getType().equals(LoadBalancerType.OUTBOUND))) {
                        throw new TestFailException("Outbound load balancer must be created but not found");
                    }
                    return testDto;
                })
                .validate();
    }

    private void createEnvironmentWithNewNetwork(TestContext testContext) {
        String telemetryKey = "telemetry";
        String networkKey = "azureNewNetwork";
        testContext
                .given(telemetryKey, TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withPublicEndpointAccessGateway(PublicEndpointAccessGateway.DISABLED)
                .withServiceEndpoints(ServiceEndpointCreation.ENABLED)
                .withSubnetIDs(null)
                .withNetworkCIDR("10.0.0.0/16")
                .withPrivateSubnets()
                .withAzure(EnvironmentNetworkAzureParamsBuilder
                        .anEnvironmentNetworkAzureParams()
                        .withNoPublicIp(true)
                        .build())
                .given(EnvironmentTestDto.class)
                .withTelemetry(telemetryKey)
                .withCreateFreeIpa(Boolean.TRUE)
                .withOneFreeIpaNode()
                .withNetwork(networkKey)
                .withTunnel(testContext.getTunnel())
                .withFreeIpaImage(commonCloudProperties().getImageValidation().getFreeIpaImageCatalog(),
                        commonCloudProperties().getImageValidation().getFreeIpaImageUuid())
                .when(environmentTestClient.create())
                .validate();
        waitForEnvironmentCreation(testContext);
        waitForUserSync(testContext);
    }
}
