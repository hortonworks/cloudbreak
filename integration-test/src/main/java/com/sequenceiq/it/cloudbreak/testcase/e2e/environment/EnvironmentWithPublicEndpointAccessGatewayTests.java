package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import static com.sequenceiq.common.api.type.PublicEndpointAccessGateway.ENABLED;
import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.AVAILABLE;
import static com.sequenceiq.it.cloudbreak.assertion.environment.EnvironmentNetworkTestAssertion.environmentWithEndpointGatewayContainsNeccessaryConfigs;
import static com.sequenceiq.it.cloudbreak.constants.NetworkConstants.NetworkConfig.SUBNET_16;
import static java.lang.Boolean.FALSE;
import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class EnvironmentWithPublicEndpointAccessGatewayTests extends AbstractE2ETest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create an Environment with public endpoint access gateway enabled and endpoint subnets provided",
            then = "should persist the user settings to the environment response")
    public void testCreateNewEnvironmentWithPublicEndpointAccessGatewayEnabled(TestContext testContext) {
        String networkKey = "someOtherNetwork";
        String telemetryKey = "someTelemetry";
        List<String> subnetIds = List.of("public-subnet-1", "public-subnet-2", "public-subnet-3");
        Set<String> workloadSubnetIds = subnetIds.stream().skip(1).collect(toSet());
        Set<String> loadBalancerSubnetIds = Set.of(subnetIds.get(0));

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(telemetryKey, TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()

                .given(EnvironmentTestDto.class)
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withNetworkCIDR(SUBNET_16.getCidr())
                .withPrivateSubnets()
                .withSubnetIDs(workloadSubnetIds)
                .withMock(new EnvironmentNetworkMockParams())
                .withPublicEndpointAccessGateway(ENABLED)
                .withEndpointGatewaySubnetIds(loadBalancerSubnetIds)
                .given(EnvironmentTestDto.class)

                .withNetwork(networkKey)
                .withTelemetry(telemetryKey)
                .withCreateFreeIpa(FALSE)
                .when(environmentTestClient.create())
                .await(AVAILABLE)
                .when(environmentTestClient.describe())
                .then(environmentWithEndpointGatewayContainsNeccessaryConfigs(workloadSubnetIds, loadBalancerSubnetIds))
                .validate();
    }

}
