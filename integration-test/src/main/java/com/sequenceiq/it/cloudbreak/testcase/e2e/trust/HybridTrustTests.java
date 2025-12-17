package com.sequenceiq.it.cloudbreak.testcase.e2e.trust;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
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

    private static final String AD_TRUST_VALIDATE_COMMAND = "nltest /dsgetdc:%s /force";

    private static final String FREEIPA_TRUST_VALIDATE_COMMAND = "export PW=$(sudo grep -v \"^#\" /srv/pillar/freeipa/init.sls | jq -r '.freeipa.password'); " +
            "kinit admin <<<$PW > /dev/null 2>&1; " +
            "kvno ldap/%s@%s";

    private static final String FAILURE_TOKEN = "===FAILURE===";

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
                .then(cleanUpActiveDirectory(false))
                .given(FreeIpaTrustCommandsDto.class)
                .when(freeIpaTestClient.trustSetupCommands())
                .then(setupActiveDirectory())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.finishTrustSetup())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .then(validateTrustOnFreeIpa())
                .then(validateTrustOnActiveDirectory())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.delete())
                .await(EnvironmentStatus.ARCHIVED)
                .given(FreeIpaTrustCommandsDto.class)
                .when(freeIpaTestClient.trustCleanupCommands())
                .then(cleanUpActiveDirectory(true))
                .validate();
    }

    private Assertion<FreeIpaTrustCommandsDto, FreeIpaClient> setupActiveDirectory() {
        return (testContext, testDto, client) -> {
            String commands = testDto.getResponse().getActiveDirectoryCommands().getCommands();
            executeActiveDirectoryCommands(testDto.getFreeIpaName() + "-setup", commands, true);
            return testDto;
        };
    }

    private Assertion<FreeIpaTrustCommandsDto, FreeIpaClient> cleanUpActiveDirectory(boolean validateError) {
        return (testContext, testDto, client) -> {
            String commands = testDto.getResponse().getActiveDirectoryCommands().getCommands();
            executeActiveDirectoryCommands(testDto.getFreeIpaName() + "-cleanup", commands, validateError);
            return testDto;
        };
    }

    private Assertion<FreeIpaTestDto, FreeIpaClient> validateTrustOnFreeIpa() {
        return (testContext, testDto, client) -> {
            List<String> freeIpaIps = testDto.getAllInstanceIps(testContext);
            TrustResponse trust = testDto.getResponse().getTrust();
            String command = String.format(FREEIPA_TRUST_VALIDATE_COMMAND, trust.getFqdn(), trust.getRealm().toUpperCase(Locale.ROOT));
            executeFreeIpaCommand(freeIpaIps, command, true);
            return testDto;
        };
    }

    private Assertion<FreeIpaTestDto, FreeIpaClient> validateTrustOnActiveDirectory() {
        return (testContext, testDto, client) ->  {
            String freeIpaInstanceFqdn = testDto.getResponse().getInstanceGroups().getFirst().getMetaData().stream()
                    .map(InstanceMetaDataResponse::getDiscoveryFQDN)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Failed to find FreeIPA instance FQDN"));
            String realm = testDto.getResponse().getFreeIpa().getDomain().toLowerCase(Locale.ROOT);
            String command = String.format(AD_TRUST_VALIDATE_COMMAND, realm);
            executeActiveDirectoryCommands(testDto.getName() + "-validate", command, true);
            return testDto;
        };
    }

    private void executeActiveDirectoryCommands(String name, String commands, boolean validateError) {
        try (SSHClient sshClient = sshJClient.createSshClient(activeDirectoryIp, activeDirectoryUser, null, commonCloudProperties.getDefaultPrivateKeyFile())) {
            String batchCommands = Arrays.stream(commands.split("\n"))
                    .map(this::wrapCommand)
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

    /**
     * Return code is not handled correctly for batch script execution on Windows Server, so as a workaround echo a special failure token when a command fails
     * Set and rem commands are excluded to make sure we are not introducing unnecessary whitespace
     */
    private String wrapCommand(String command) {
        return '\''
                + (StringUtils.isNotEmpty(command) && !command.startsWith("set") && !command.startsWith("REM")
                        ? String.format("%s || echo: && echo %s", command, FAILURE_TOKEN)
                        : command)
                + '\'';
    }

    private void executeFreeIpaCommand(List<String> freeIpaIps, String command, boolean validateError) {
        freeIpaIps.forEach(freeIpaIp -> {
            Pair<Integer, String> result = sshJClient.executeCommand(freeIpaIp, command);
            validateCommandResult(command, result, validateError);
        });
    }

    private void validateCommandResult(String command, Pair<Integer, String> sshResult, boolean validateError) {
        Integer returnCode = sshResult.getLeft();
        String output = sshResult.getRight();
        LOGGER.info("Result of the {} command:\nReturn code: {}\nOutput: {}", command, returnCode, output);
        if (validateError && (returnCode != 0 || Arrays.asList(Objects.requireNonNullElse(output, "").split("\r?\n")).contains(FAILURE_TOKEN))) {
            throw new TestFailException("The command execution of '" + command + "' failed.");
        }
    }
}
