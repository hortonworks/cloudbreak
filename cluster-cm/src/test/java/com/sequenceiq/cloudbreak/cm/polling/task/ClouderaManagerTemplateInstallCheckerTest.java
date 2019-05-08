package com.sequenceiq.cloudbreak.cm.polling.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiCommandList;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerClientFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@ExtendWith(MockitoExtension.class)
class ClouderaManagerTemplateInstallCheckerTest {

    private static final BigDecimal TEMPLATE_INSTALL_ID = new BigDecimal(1);

    private static final BigDecimal ADD_REPOSITORIES_ID = new BigDecimal(11);

    private static final BigDecimal DEPLOY_PARCELS_ID = new BigDecimal(12);

    @InjectMocks
    private ClouderaManagerTemplateInstallChecker underTest;

    @Mock
    private ApiClient apiClient;

    @Mock
    private ClouderaManagerClientFactory clouderaManagerClientFactory;

    @Mock
    private CommandsResourceApi commandsResourceApi;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        when(clouderaManagerClientFactory.getCommandsResourceApi(eq(apiClient))).thenReturn(commandsResourceApi);
    }

    @Test
    void checkStatus() throws ApiException {
        ClouderaManagerCommandPollerObject pollerObject = new ClouderaManagerCommandPollerObject(new Stack(), apiClient, TEMPLATE_INSTALL_ID);

        ApiCommand addRepositoriesChildCmd = new ApiCommand().id(ADD_REPOSITORIES_ID).active(Boolean.FALSE).name("AddRepositories").success(Boolean.TRUE);
        ApiCommand deployParcelsChildCmd = new ApiCommand().id(DEPLOY_PARCELS_ID).active(Boolean.FALSE).name("DeployParcels").success(Boolean.FALSE);
        ApiCommandList firstLevelCmdList = new ApiCommandList().items(List.of(addRepositoriesChildCmd, deployParcelsChildCmd));

        ApiCommand templateInstallCommand = new ApiCommand().id(TEMPLATE_INSTALL_ID).active(Boolean.FALSE).success(Boolean.FALSE).children(firstLevelCmdList);
        ApiCommand deployParcelsCmd = deployParcelsChildCmd.resultMessage("Failed to deploy parcels");
        when(commandsResourceApi.readCommand(any(BigDecimal.class))).thenAnswer(invocation -> {
            BigDecimal cmdId = invocation.getArgument(0);
            if (TEMPLATE_INSTALL_ID.compareTo(cmdId) == 0) {
                return templateInstallCommand;
            } else if (DEPLOY_PARCELS_ID.compareTo(cmdId) == 0) {
                return deployParcelsCmd;
            } else {
                throw new IllegalArgumentException("Uexpected argument for readCommand: " + cmdId);
            }
        });

        ClouderaManagerOperationFailedException ex = assertThrows(ClouderaManagerOperationFailedException.class, () -> underTest.checkStatus(pollerObject));
        String expected = "Cluster template install failed: [Command [DeployParcels], with id [12] failed: Failed to deploy parcels]";
        assertEquals(expected, ex.getMessage());
    }
}