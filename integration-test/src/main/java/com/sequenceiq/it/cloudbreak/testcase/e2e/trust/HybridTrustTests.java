package com.sequenceiq.it.cloudbreak.testcase.e2e.trust;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.sequenceiq.common.api.type.ConfigStalenessState;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.assertion.hybrid.HybridTrustAssertions;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RemoteEnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXInstanceGroupTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentDirectionalTrustSetupDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTrustSetupDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDirectionalTrustCommandsDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTrustCommandsDto;
import com.sequenceiq.it.cloudbreak.dto.remoteenvironment.DescribeRemoteEnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.action.ActiveDirectorySshJClientActions;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

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

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        testContext
                .given(DescribeRemoteEnvironmentTestDto.class)
                .when(remoteEnvironmentTestClient.describe())
                .then((tc, testDto, client) -> {
                    PrivateDatalakeDetails datalake = testDto.getResponse().getEnvironment()
                            .getPvcEnvironmentDetails().getPrivateDatalakeDetails();
                    if (datalake == null || datalake.getStatus() != PrivateDatalakeDetails.StatusEnum.AVAILABLE) {
                        String status = datalake != null ? String.valueOf(datalake.getStatus()) : "null";
                        throw new TestFailException(
                                "Classic cluster CM is not available (status: " + status
                                        + "). Trust setup will fail — check if Cloudera Manager is running on the on-prem cluster.");
                    }
                    LOGGER.info("Classic cluster CM healthcheck passed, datalake status: {}", datalake.getStatus());
                    return testDto;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create a hybrid environment, setup trust with the given active directory",
            then = "trust setup successfully finished")
    public void testTrustSetup(TestContext testContext) {
        AtomicReference<String> runtimeVersion = new AtomicReference<>();

        testContext
                .given("telemetry", TelemetryTestDto.class)
                    .withLogging()
                    .withReportClusterLogs()
                .given(EnvironmentTestDto.class)
                    .withTelemetry("telemetry")
                    .withCreateFreeIpa(Boolean.TRUE)
                    .withOneFreeIpaNode()
                    .withEnvironmentType(EnvironmentType.HYBRID)
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
                .given(DescribeRemoteEnvironmentTestDto.class)
                .when(remoteEnvironmentTestClient.describe())
                .then((tc, testDto, client) -> {
                    runtimeVersion.set(testDto.getResponse().getEnvironment().getCdpRuntimeVersion().split("-")[0]);
                    return testDto;
                })
                .given(DistroXTestDto.class)
                    .withTemplate(commonClusterManagerProperties().getHybridDataMartDistroXBlueprintName(runtimeVersion.get()))
                    .withInstanceGroupsEntity(DistroXInstanceGroupTestDto.dataMartHostGroups(testContext))
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then(hybridTrustAssertions.validateTwoWayTrustOnDistroX())
                .given(FreeIpaTrustCommandsDto.class)
                .when(freeIpaTestClient.trustCleanupCommands(), RunningParameter.force())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete(), RunningParameter.force())
                .await(EnvironmentStatus.ARCHIVED)
                .given(FreeIpaTrustCommandsDto.class)
                .then(cleanUpActiveDirectory(false), RunningParameter.force())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "an existing public cloud environment",
            when = "setup trust with the given active directory",
            then = "trust setup successfully finished and the cluster configurations become stale, then up to date again after service restart")
    public void testTrustSetupExistingEnv(TestContext testContext) {
        // set a prefix so the domain's will not collide with the other trust test, allowing parallel runs with same Active Directory
        testContext
                .given(EnvironmentTestDto.class)
                .withName(resourcePropertyProvider.getEnvironmentName("existingenv"))
                .withTrustSetup();
        createDefaultEnvironment(testContext);
        createDatalakeWithoutDatabase(testContext);
        testContext
                .given(EnvironmentDirectionalTrustSetupDto.class)
                .when(environmentTestClient.setupDirectionalTrust())
                .awaitForFlow()
                .given(FreeIpaTrustCommandsDto.class)
                .when(freeIpaTestClient.trustCleanupCommands())
                .then(cleanUpActiveDirectory(true))
                .given(FreeIpaDirectionalTrustCommandsDto.class)
                .when(freeIpaTestClient.directionalTrustSetupCommands())
                .then(setupActiveDirectoryOneWay())
                .given(SdxInternalTestDto.class)
                .awaitConfigStalenessState(SdxClusterStatusResponse.RUNNING, ConfigStalenessState.STALE)
                .when(sdxTestClient.restartClusterServices(false, true))
                .awaitConfigStalenessState(SdxClusterStatusResponse.RUNNING, ConfigStalenessState.RESTART_IN_PROGRESS)
                .awaitForFlow()
                .awaitConfigStalenessState(SdxClusterStatusResponse.RUNNING, ConfigStalenessState.UP_TO_DATE)
                .then(hybridTrustAssertions.validateOneWayTrustOnSdx())
//                .given(EnvironmentTestDto.class)
//                .when(environmentTestClient.finishTrustSetup())
//                .await(EnvironmentStatus.AVAILABLE)
//                .given(FreeIpaDirectionalTrustCommandsDto.class)
//                .when(freeIpaTestClient.directionalTrustSetupCommands())
//                .then(setupActiveDirectoryTwoWay())
//                .given(SdxInternalTestDto.class)
//                .then(hybridTrustAssertions.validateTwoWayTrustOnSdx())
                .given(FreeIpaTrustCommandsDto.class)
                .when(freeIpaTestClient.trustCleanupCommands(), RunningParameter.force())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete(), RunningParameter.force())
                .await(EnvironmentStatus.ARCHIVED)
                .given(FreeIpaTrustCommandsDto.class)
                .then(cleanUpActiveDirectory(false), RunningParameter.force())
                .validate();
    }

    private Assertion<FreeIpaTrustCommandsDto, FreeIpaClient> setupActiveDirectory() {
        return (testContext, testDto, client) -> {
            String commands = testDto.getResponse().getActiveDirectoryCommands().getCommands();
            activeDirectorySshJClientActions.executeActiveDirectoryCommands(testDto.getFreeIpaName() + "-setup", commands, true);
            return testDto;
        };
    }

    private Assertion<FreeIpaDirectionalTrustCommandsDto, FreeIpaClient> setupActiveDirectoryOneWay() {
        return (testContext, testDto, client) -> {
            String commands = testDto.getResponse().getOneWay().getActiveDirectoryCommands().getCommands();
            activeDirectorySshJClientActions.executeActiveDirectoryCommands(testDto.getFreeIpaName() + "-setup", commands, true);
            return testDto;
        };
    }

    private Assertion<FreeIpaDirectionalTrustCommandsDto, FreeIpaClient> setupActiveDirectoryTwoWay() {
        return (testContext, testDto, client) -> {
            String commands = testDto.getResponse().getTwoWay().getActiveDirectoryCommands().getCommands();
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
