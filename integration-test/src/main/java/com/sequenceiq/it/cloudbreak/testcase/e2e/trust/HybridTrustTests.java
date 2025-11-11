package com.sequenceiq.it.cloudbreak.testcase.e2e.trust;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTrustSetupDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTrustCommandsDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;

import net.schmizz.sshj.SSHClient;

public class HybridTrustTests extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(HybridTrustTests.class);

    private static final String SET_CONTENT_COMMAND = "Set-Content -Path $env:TEMP\\%s -Value %s";

    private static final String EXECUTE_BATCH_COMMAND = "%%TEMP%%\\%s";

    private static final String DELETE_BATCH_COMMAND = "del /f /q %%TEMP%%\\%s";

    private static final String BATCH_FILENAME = "trustsetup-%s.bat";

    @Value("${integrationtest.trust.activedirectory.ip}")
    private String activeDirectoryIp;

    @Value("${integrationtest.trust.activedirectory.user}")
    private String activeDirectoryUser;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SshJClient sshJClient;

    @Inject
    private CommonCloudProperties commonCloudProperties;

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
                .await(EnvironmentStatus.TRUST_SETUP_REQUIRED)
                .awaitForFlow()
                .when(environmentTestClient.describe())
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .given(EnvironmentTrustSetupDto.class)
                .when(environmentTestClient.setupTrust())
                .await(EnvironmentStatus.TRUST_SETUP_FINISH_REQUIRED)
                .given(FreeIpaTrustCommandsDto.class)
                .when(freeIpaTestClient.trustCleanupCommands())
                .then((tc, dto, client) -> cleanUpActiveDirectory(dto, false))
                .given(FreeIpaTestDto.class)
                .then((tc, dto, client) -> setupActiveDirectory(dto, tc, client))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.finishTrustSetup())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.delete())
                .await(EnvironmentStatus.ARCHIVED)
                .given(FreeIpaTrustCommandsDto.class)
                .then((tc, dto, client) -> cleanUpActiveDirectory(dto, true))
                .validate();
    }

    private FreeIpaTestDto setupActiveDirectory(FreeIpaTestDto freeipaTestDto, TestContext testContext, FreeIpaClient freeipaClient) {
        TrustSetupCommandsResponse response = freeipaClient.getDefaultClient().getTrustV1Endpoint()
                .getTrustSetupCommands(testContext.get(EnvironmentTestDto.class).getResponse().getCrn());
        executeCommands(freeipaTestDto.getRequest().getName(), response.getActiveDirectoryCommands().getCommands(), true);
        return freeipaTestDto;
    }

    private FreeIpaTrustCommandsDto cleanUpActiveDirectory(FreeIpaTrustCommandsDto freeipaTestDto, boolean validateError) {
        executeCommands(freeipaTestDto.getFreeIpaName() + "-cleanup", freeipaTestDto.getResponse().getActiveDirectoryCommands().getCommands(), validateError);
        return freeipaTestDto;
    }

    private void executeCommands(String name, String commands, boolean validateError) {
        try (SSHClient sshClient = sshJClient.createSshClient(activeDirectoryIp, activeDirectoryUser, null, commonCloudProperties.getDefaultPrivateKeyFile())) {
            String batchCommands = Arrays.stream(commands.split("\n"))
                    .map(command -> "'" + command + "'")
                    .collect(Collectors.joining(","));
            String batchFileName = String.format(BATCH_FILENAME, name);
            String uploadBatchCommand = "powershell -EncodedCommand " + Base64.getEncoder().encodeToString(
                    String.format(SET_CONTENT_COMMAND, batchFileName, batchCommands).getBytes(StandardCharsets.UTF_16LE));
            Pair<Integer, String> createScriptResult = sshJClient.execute(sshClient, uploadBatchCommand, 120L);
            validateCommandResult(uploadBatchCommand, createScriptResult, validateError);
            String executeBatchCommand = String.format(EXECUTE_BATCH_COMMAND, batchFileName);
            Pair<Integer, String> executeScriptResult = sshJClient.execute(sshClient, executeBatchCommand, 120L);
            validateCommandResult(executeBatchCommand, executeScriptResult, validateError);
            String deleteBatchCommand = String.format(DELETE_BATCH_COMMAND, batchFileName);
            Pair<Integer, String> deleteScriptResult = sshJClient.execute(sshClient, deleteBatchCommand, 120L);
            validateCommandResult(deleteBatchCommand, deleteScriptResult, false);
        } catch (IOException ex) {
            String errorMessage = String.format("Exception during ssh to active directory %s", activeDirectoryIp);
            LOGGER.error(errorMessage, ex);
            throw new TestFailException(errorMessage, ex);
        }
    }

    private void validateCommandResult(String command, Pair<Integer, String> sshResult, boolean validateError) {
        LOGGER.info("Result of the {} command:\n{}", command, sshResult);
        if (validateError && sshResult.getLeft() != 0) {
            throw new TestFailException("The command execution of '" + command + "' failed. Return code is " + sshResult.getLeft());
        }
    }
}
