package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.error.mapper.ClouderaManagerErrorMapperService;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerTemplateInstallationCheckerTest {

    private static final BigDecimal TEMPLATE_INSTALL_ID = new BigDecimal(1);

    private static final BigDecimal ADD_REPOSITORIES_ID = new BigDecimal(11);

    private static final BigDecimal DEPLOY_PARCELS_ID = new BigDecimal(12);

    private static final BigDecimal FIRST_RUN_ID = new BigDecimal(13);

    private static final BigDecimal AUDIT_DIR = new BigDecimal(14);

    private static final String TEMPLATE_INSTALL_NAME = "TemplateInstall";

    private static final String ADD_REPOSITORIES_NAME = "AddRepositories";

    private static final String DEPLOY_PARCELS_NAME = "DeployParcels";

    private static final String FIRST_RUN_NAME = "First Run";

    private static final String AUDIT_DIR_COMMAND_NAME = "CreateRangerKafkaPluginAuditDirCommand";

    @Mock
    private ApiClient apiClient;

    @Mock
    private ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory;

    @Mock
    private CommandsResourceApi commandsResourceApi;

    @Mock
    private ApiCommand apiCommand;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private ClouderaManagerErrorMapperService clouderaManagerErrorMapperService;

    @InjectMocks
    private ClouderaManagerTemplateInstallationChecker underTest;

    private ClouderaManagerCommandPollerObject pollerObject;

    private Stack stack;

    @BeforeEach
    void setUp() throws ApiException {
        when(commandsResourceApi.readCommand(any())).thenReturn(apiCommand);
        when(clouderaManagerApiPojoFactory.getCommandsResourceApi(eq(apiClient))).thenReturn(commandsResourceApi);
        stack = new Stack();
        stack.setType(StackType.DATALAKE);
        pollerObject = new ClouderaManagerCommandPollerObject(stack, apiClient, TEMPLATE_INSTALL_ID);
        lenient().when(clouderaManagerErrorMapperService.map(eq(stack), any(), any())).thenAnswer(i -> i.getArgument(2));
    }

    @Test
    void checkStatusWithSubCommandMessage() throws ApiException {
        ApiCommand addRepositoriesCmd = addReposCmd();
        ApiCommand deployParcelsCmd = deployParcelsCmd().success(Boolean.FALSE)
                .resultMessage("Failed to deploy parcels");
        ApiCommand templateInstallCmd = templateInstallCmd(addRepositoriesCmd, deployParcelsCmd);
        expectReadCommandForFailedCommands(templateInstallCmd);

        ClouderaManagerOperationFailedException ex = assertThrowsExact(ClouderaManagerOperationFailedException.class,
                () -> underTest.checkStatus(pollerObject));
        String expected = expectMessageForCommands(deployParcelsCmd);
        assertEquals(expected, ex.getMessage());
    }

    // Ensures an exact class match in order to avoid accepting a subclass. So, e.g., CloudStorageConfigurationFailedException (which is a subclass of
    // ClouderaManagerOperationFailedException) will not match as a ClouderaManagerOperationFailedException.
    private <T extends Throwable> T assertThrowsExact(Class<T> expectedType, Executable executable) {
        T ex = assertThrows(expectedType, executable);
        assertThat(ex.getClass()).isSameAs(expectedType);
        return ex;
    }

    @Test
    void checkStatusWithMultiLevelMessage() throws ApiException {
        ApiCommand addRepositoriesCmd = addReposCmd();
        ApiCommand deployParcelsCmd = deployParcelsCmd();
        ApiCommand firstRunCmd = firstRunCmd().success(Boolean.FALSE)
                .resultMessage("Failed to perform First Run of services.");
        ApiCommand templateInstallCmd = templateInstallCmd(addRepositoriesCmd, deployParcelsCmd, firstRunCmd)
                .resultMessage("Failed to import cluster template");
        expectReadCommandForFailedCommands(templateInstallCmd);

        ClouderaManagerOperationFailedException ex = assertThrowsExact(ClouderaManagerOperationFailedException.class,
                () -> underTest.checkStatus(pollerObject));
        String expected = expectMessageForCommands(firstRunCmd);
        assertEquals(expected, ex.getMessage());
    }

    @Test
    void checkStatusWithMultipleFailures() throws ApiException {
        ApiCommand addRepositoriesCmd = addReposCmd().success(Boolean.FALSE)
                .resultMessage("Permission denied");
        ApiCommand deployParcelsCmd = deployParcelsCmd().success(Boolean.FALSE)
                .resultMessage("Host not found");
        ApiCommand firstRunCmd = firstRunCmd().success(Boolean.FALSE)
                .resultMessage("Failed to perform First Run of services.");
        ApiCommand templateInstallCmd = templateInstallCmd(addRepositoriesCmd, deployParcelsCmd, firstRunCmd)
                .resultMessage("Failed to import cluster template");
        expectReadCommandForFailedCommands(templateInstallCmd);

        ClouderaManagerOperationFailedException ex = assertThrowsExact(ClouderaManagerOperationFailedException.class,
                () -> underTest.checkStatus(pollerObject));
        String expected = expectMessageForCommands(addRepositoriesCmd, deployParcelsCmd, firstRunCmd);
        assertEquals(expected, ex.getMessage());
    }

    @Test
    void checkStatusWithTopLevelMessage() throws ApiException {
        ApiCommand addRepositoriesCmd = addReposCmd();
        ApiCommand deployParcelsCmd = deployParcelsCmd();
        ApiCommand templateInstallCmd = templateInstallCmd(addRepositoriesCmd, deployParcelsCmd)
                .resultMessage("IllegalArgumentException: Unknown configuration attribute 'process_autorestart_enabled'.");
        expectReadCommandForFailedCommands(templateInstallCmd);

        ClouderaManagerOperationFailedException ex = assertThrowsExact(ClouderaManagerOperationFailedException.class,
                () -> underTest.checkStatus(pollerObject));
        String expected = expectMessageForCommands(templateInstallCmd);
        assertEquals(expected, ex.getMessage());
    }

    @Test
    void checkStatusWithActiveCommands() throws ApiException {
        ApiCommand addRepositoriesCmd = addReposCmd().success(Boolean.FALSE)
                .resultMessage("Permission denied");
        ApiCommand deployParcelsCmd = deployParcelsCmd().success(Boolean.FALSE)
                .resultMessage("Host not found");
        ApiCommand firstRunCmd = firstRunCmd().active(Boolean.TRUE)
                .resultMessage("Actually this has not finished yet...");
        ApiCommand templateInstallCmd = templateInstallCmd(addRepositoriesCmd, deployParcelsCmd, firstRunCmd)
                .resultMessage("Failed to import cluster template");
        expectReadCommandForFailedCommands(templateInstallCmd);

        ClouderaManagerOperationFailedException ex = assertThrowsExact(ClouderaManagerOperationFailedException.class,
                () -> underTest.checkStatus(pollerObject));
        String expected = expectMessageForCommands(addRepositoriesCmd, deployParcelsCmd, firstRunCmd);
        assertEquals(expected, ex.getMessage());
    }

    @Test
    void testErrorMessageMapping() throws ApiException {
        ApiCommand auditDirCmd = auditDirCmd().success(Boolean.FALSE)
                .resultMessage("Aborted");
        ApiCommand deployParcelsCmd = deployParcelsCmd().success(Boolean.FALSE)
                .resultMessage("Host not found");
        ApiCommand firstRunCmd = firstRunCmd().success(Boolean.FALSE)
                .resultMessage("Failed to perform First Run of services.");
        ApiCommand templateInstallCmd = templateInstallCmd(auditDirCmd, deployParcelsCmd, firstRunCmd)
                .resultMessage("Failed to import cluster template");
        expectReadCommandForFailedCommands(templateInstallCmd);
        String expected = "Mapped error message";
        when(clouderaManagerErrorMapperService.map(eq(stack), any(), any())).thenReturn(expected);

        ClouderaManagerOperationFailedException ex = assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.checkStatus(pollerObject));
        assertEquals(expected, ex.getMessage());
    }

    @Test
    void testCloudStorageFailureWhenDatahub() throws ApiException {
        ApiCommand auditDirCmd = auditDirCmd().success(Boolean.FALSE)
                .resultMessage("Aborted");
        ApiCommand deployParcelsCmd = deployParcelsCmd().success(Boolean.FALSE)
                .resultMessage("Host not found");
        ApiCommand firstRunCmd = firstRunCmd().success(Boolean.FALSE)
                .resultMessage("Failed to perform First Run of services.");
        ApiCommand templateInstallCmd = templateInstallCmd(auditDirCmd, deployParcelsCmd, firstRunCmd)
                .resultMessage("Failed to import cluster template");
        expectReadCommandForFailedCommands(templateInstallCmd);

        stack.setType(StackType.WORKLOAD);

        ClouderaManagerOperationFailedException ex = assertThrowsExact(ClouderaManagerOperationFailedException.class,
                () -> underTest.checkStatus(pollerObject));
        String expected = expectMessageForCommands(auditDirCmd, deployParcelsCmd, firstRunCmd);
        assertEquals(expected, ex.getMessage());
    }

    private void expectReadCommandForFailedCommands(ApiCommand templateInstallCmd) throws ApiException {
        Map<BigDecimal, ApiCommand> failedCommands = Stream.concat(
                Stream.of(templateInstallCmd),
                templateInstallCmd.getChildren().getItems().stream()
        )
                .filter(cmd -> cmd.isActive() != null && cmd.isActive() || cmd.isSuccess() != null && !cmd.isSuccess())
                .collect(Collectors.toMap(ApiCommand::getId, Function.identity()));

        when(commandsResourceApi.readCommand(any(BigDecimal.class))).thenAnswer(invocation -> {
            BigDecimal cmdId = invocation.getArgument(0);
            ApiCommand cmd = failedCommands.get(cmdId);
            if (cmd == null) {
                throw new IllegalArgumentException("Unexpected argument for readCommand: " + cmdId);
            }
            return cmd;
        });
    }

    private String expectMessageForCommands(ApiCommand... commands) {
        String msgFormat = "Installation of CDP with Cloudera Manager has failed. Please find more details on Cloudera Manager UI. Failed command(s): %s";
        String cmdFormat = "%s(id=%d): %s";
        return String.format(msgFormat,
                Arrays.stream(commands)
                        .map(cmd -> String.format(cmdFormat, cmd.getName(), cmd.getId().intValue(), cmd.getResultMessage()))
                        .collect(Collectors.joining(" "))
        );
    }

    private ApiCommand templateInstallCmd(ApiCommand... children) {
        return cmd(TEMPLATE_INSTALL_ID, TEMPLATE_INSTALL_NAME)
                .active(Boolean.FALSE)
                .success(Boolean.FALSE)
                .children(new ApiCommandList().items(List.of(children)));
    }

    private ApiCommand auditDirCmd() {
        return cmd(AUDIT_DIR, AUDIT_DIR_COMMAND_NAME);
    }

    private ApiCommand addReposCmd() {
        return cmd(ADD_REPOSITORIES_ID, ADD_REPOSITORIES_NAME);
    }

    private ApiCommand deployParcelsCmd() {
        return cmd(DEPLOY_PARCELS_ID, DEPLOY_PARCELS_NAME);
    }

    private ApiCommand firstRunCmd() {
        return cmd(FIRST_RUN_ID, FIRST_RUN_NAME);
    }

    private ApiCommand cmd(BigDecimal id, String name) {
        return new ApiCommand().id(id).name(name).active(Boolean.FALSE).success(Boolean.TRUE);
    }
}
