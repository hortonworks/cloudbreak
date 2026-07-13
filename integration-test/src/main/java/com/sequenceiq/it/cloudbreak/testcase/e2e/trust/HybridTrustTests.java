package com.sequenceiq.it.cloudbreak.testcase.e2e.trust;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.hybrid.HybridTrustAssertions;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RemoteEnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTrustSetupDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTrustCommandsDto;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.DescribeRemoteEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.ActiveDirectorySshJClientActions;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

public class HybridTrustTests extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HybridTrustTests.class);

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private RemoteEnvironmentTestClient remoteEnvironmentTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private SshJClient sshJClient;

    @Inject
    private ActiveDirectorySshJClientActions activeDirectorySshJClientActions;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private HybridTrustAssertions hybridTrustAssertions;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create a hybrid environment, setup trust with the given active directory",
            then = "trust setup successfully finished")
    public void testTrustSetup(TestContext testContext) {
        DescribeEnvironmentResponse remoteEnvironment = testContext
                .given(DescribeRemoteEnvironmentTestDto.class)
                .when(remoteEnvironmentTestClient.describe())
                .getResponse();
        String runtimeVersion = remoteEnvironment.getEnvironment().getCdpRuntimeVersion().split("-")[0];

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withTelemetry("telemetry")
                    .withCreateFreeIpa(Boolean.TRUE)
                    .withOneFreeIpaNode()
                    .withTrustSetup()
                .when(environmentTestClient.create())
                .awaitForHybridCreationFlow()
                .refresh()
                .given(FreeIpaTestDto.class)
                .refresh()
                .given(EnvironmentTrustSetupDto.class)
                .when(environmentTestClient.setupTrust())
                .await(EnvironmentStatus.TRUST_SETUP_FINISH_REQUIRED)
                .given(FreeIpaTrustCommandsDto.class)
                .when(freeIpaTestClient.trustCleanupCommands())
                .then(cleanUpActiveDirectory(true))
                .given(FreeIpaTrustCommandsDto.class)
                .when(freeIpaTestClient.trustSetupCommands())
                .then(setupActiveDirectory())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.finishTrustSetup())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .refresh()
                .then(hybridTrustAssertions.validateTrustOnFreeIpa())
                .then(hybridTrustAssertions.validateTrustOnActiveDirectory())
                .given(DistroXTestDto.class)
                    .withTemplate(commonClusterManagerProperties().getHybridDataMartDistroXBlueprintName(runtimeVersion))
                    .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.dataMartHostGroups(testContext))
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then(hybridTrustAssertions.validateTrustOnDistroX())
                .given(FreeIpaTrustCommandsDto.class)
                .when(freeIpaTestClient.trustCleanupCommands())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete())
                .await(EnvironmentStatus.ARCHIVED)
                .given(FreeIpaTrustCommandsDto.class)
                .then(cleanUpActiveDirectory(false))
                .validate();
    }

    private Assertion<FreeIpaTrustCommandsDto, FreeIpaClient> setupActiveDirectory() {
        return (testContext, testDto, client) -> {
            String commands = testDto.getResponse().getActiveDirectoryCommands().getCommands();
            activeDirectorySshJClientActions.executeActiveDirectoryCommands(testDto.getFreeIpaName() + "-setup", commands, true);
            return testDto;
        };
    }

    private Assertion<FreeIpaTrustCommandsDto, FreeIpaClient> cleanUpActiveDirectory(boolean validateError) {
        return (testContext, testDto, client) -> {
            String commands = testDto.getResponse().getActiveDirectoryCommands().getCommands();
            activeDirectorySshJClientActions.executeActiveDirectoryCommands(testDto.getFreeIpaName() + "-cleanup", commands, validateError);
            return testDto;
        };
    }
}
