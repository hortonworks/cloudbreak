package com.sequenceiq.it.cloudbreak.util.ssh.action;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.config.TrustProperties;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.util.ssh.SshCommandOutcome;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClient;
import com.sequenceiq.it.cloudbreak.util.ssh.client.SshJClientFactory;
import com.sequenceiq.it.util.SimpleRetryWrapper;

import net.schmizz.sshj.SSHClient;

@Component
public class ActiveDirectorySshJClientActions {

    /**
     * Make sure that the Active Directory scripts validated for return code write this token in a new line in case of a failure
     */
    public static final String FAILURE_TOKEN = "[FAILURE]";

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveDirectorySshJClientActions.class);

    private static final String SET_CONTENT_COMMAND = "Set-Content -Path $env:TEMP\\%s -Value %s";

    private static final String EXECUTE_BATCH_COMMAND = "%%TEMP%%\\%s";

    private static final String DELETE_BATCH_COMMAND = "del /f /q %%TEMP%%\\%s";

    private static final String BATCH_FILENAME = "trustsetup-%s.bat";

    private static final long EXIT_STATUS_WAIT_SEC = 120L;

    @Inject
    private TrustProperties trustProperties;

    @Inject
    private SshJClientFactory sshJClientFactory;

    @Inject
    private SshJClient sshJClient;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    public void executeActiveDirectoryCommands(String name, String commands, boolean validateError) {
        try (SSHClient sshClient = sshJClientFactory.createSshClient(
                trustProperties.getActiveDirectoryIp(), trustProperties.getActiveDirectoryUser(), null, commonCloudProperties.getDefaultPrivateKeyFile())) {
            String batchCommands = Arrays.stream(commands.split("\n"))
                    .map(command -> '\'' + command + '\'')
                    .collect(Collectors.joining(","));
            String batchFileName = String.format(BATCH_FILENAME, name);
            String uploadBatchCommand = "powershell -EncodedCommand " + Base64.getEncoder().encodeToString(
                    String.format(SET_CONTENT_COMMAND, batchFileName, batchCommands).getBytes(StandardCharsets.UTF_16LE));
            SshCommandOutcome createScriptResult = sshJClient.execute(sshClient, trustProperties.getActiveDirectoryIp(), uploadBatchCommand,
                    EXIT_STATUS_WAIT_SEC);
            validateCommandResult(uploadBatchCommand, createScriptResult, validateError);

            SimpleRetryWrapper.create(() -> {
                try {
                    String executeBatchCommand = String.format(EXECUTE_BATCH_COMMAND, batchFileName);
                    SshCommandOutcome executeScriptResult = sshJClient.execute(sshClient, trustProperties.getActiveDirectoryIp(), executeBatchCommand,
                            EXIT_STATUS_WAIT_SEC);
                    validateCommandResult(executeBatchCommand, executeScriptResult, validateError);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).withName(String.format("execute %s batch commands", name)).withRetryWaitSeconds(30).run();

            String deleteBatchCommand = String.format(DELETE_BATCH_COMMAND, batchFileName);
            SshCommandOutcome deleteScriptResult = sshJClient.execute(sshClient, trustProperties.getActiveDirectoryIp(), deleteBatchCommand,
                    EXIT_STATUS_WAIT_SEC);
            validateCommandResult(deleteBatchCommand, deleteScriptResult, false);
        } catch (IOException ex) {
            String errorMessage = String.format("Exception during ssh to active directory %s", trustProperties.getActiveDirectoryIp());
            LOGGER.error(errorMessage, ex);
            throw new TestFailException(errorMessage, ex);
        }
    }

    private void validateCommandResult(String command, SshCommandOutcome sshResult, boolean validateError) {
        String output = sshResult.commandOutput();
        LOGGER.info("Result of the {} command:\nReturn code: {}\nOutput: {}", command, sshResult.returnCode(), output);
        if (validateError && (!sshResult.returnCodeSuccess() || containsFailureToken(output))) {
            throw new TestFailException("The command execution of '" + command + "' failed.");
        }
    }

    private boolean containsFailureToken(String output) {
        return Arrays.stream(Objects.requireNonNullElse(output, "").split("\r?\n"))
                .anyMatch(line -> line.startsWith(FAILURE_TOKEN));
    }
}
