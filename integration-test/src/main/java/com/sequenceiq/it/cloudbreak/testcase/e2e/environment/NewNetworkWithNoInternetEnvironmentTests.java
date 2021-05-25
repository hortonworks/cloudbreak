package com.sequenceiq.it.cloudbreak.testcase.e2e.environment;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.assertion.environment.EnvironmentNetworkTestAssertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.SshJClientActions;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class NewNetworkWithNoInternetEnvironmentTests extends AbstractE2ETest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SshJClientActions sshJClientActions;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create an Environment with new network which CIDR is 10.0.0.0/16, outbound internet traffic is disabled with FreeIPA and SDX",
            then = "should create new Subnets and the number depends on the provider and should create instances without internet access.")
    public void testCreateNewEnvironmentWithNewNetworkAndNoInternet(TestContext testContext) {
        String networkKey = "someOtherNetwork";
        String sdx = resourcePropertyProvider().getName();
        SdxDatabaseRequest database = new SdxDatabaseRequest();
        database.setCreate(false);

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
                .withNoOutboundInternetTraffic()
                .withServiceEndpoints()

                .given(EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withTelemetry("telemetry")
                .withCreateFreeIpa(Boolean.TRUE)
                .withClusterProxy()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .then((tc, testDto, cc) -> environmentTestClient.describe().action(tc, testDto, cc))
                .then(EnvironmentNetworkTestAssertion.environmentContainsNeccessaryConfigs())

                .init(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .then((tc, testDto, client) -> sshJClientActions.checkNoOutboundInternetTraffic(testDto, client))

                .given(sdx, SdxTestDto.class)
                .withExternalDatabase(database)
                .withCloudStorage()
                .when(sdxTestClient.create(), RunningParameter.key(sdx))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> sshJClientActions.checkNoOutboundInternetTraffic(testDto, getInstanceGroups(testDto, client),
                        List.of(HostGroupType.MASTER.getName())))
                .validate();
    }

    private List<InstanceGroupV4Response> getInstanceGroups(SdxTestDto testDto, SdxClient client) {
        return client.getDefaultClient()
                .sdxEndpoint()
                .getDetailByCrn(testDto.getCrn(), Collections.emptySet())
                .getStackV4Response().getInstanceGroups();
    }
}
