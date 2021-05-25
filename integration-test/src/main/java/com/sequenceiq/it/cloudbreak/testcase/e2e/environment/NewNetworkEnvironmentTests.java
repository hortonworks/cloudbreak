package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.environment.EnvironmentNetworkTestAssertion;
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

public class NewNetworkEnvironmentTests extends AbstractE2ETest {

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
            when = "create an Environment with new network which CIDR is 10.0.0.0/16 without FreeIPA",
            then = "should create new Subnets and the number depends on the provider")
    public void testCreateNewEnvironmentWithNewNetwork(TestContext testContext) {
        String networkKey = "someOtherNetwork";

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()

                .given(EnvironmentTestDto.class)
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withNetworkCIDR("10.0.0.0/16")
                .withPrivateSubnets()
                .given(EnvironmentTestDto.class)

                .withNetwork(networkKey)
                .withTelemetry("telemetry")
                .withCreateFreeIpa(Boolean.FALSE)
                .withClusterProxy()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .then((tc, testDto, cc) -> environmentTestClient.describe().action(tc, testDto, cc))
                .then(EnvironmentNetworkTestAssertion.environmentContainsNeccessaryConfigs())
                .validate();
    }
}
