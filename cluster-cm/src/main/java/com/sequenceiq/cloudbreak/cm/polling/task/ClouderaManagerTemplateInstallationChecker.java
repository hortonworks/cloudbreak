package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.exception.CloudStorageConfigurationFailedException;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetailsFormatter;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.cm.util.ClouderaManagerCommandUtil;

public class ClouderaManagerTemplateInstallationChecker extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Set<String> CLOUD_STORAGE_RELATED_COMMANDS = Set.of(
            "RangerPluginCreateAuditDir",
            "CreateRangerAuditDir",
            "CreateRangerKafkaPluginAuditDirCommand",
            "CreateHiveWarehouseExternalDir",
            "CreateHiveWarehouseDir",
            "CreateRangerKnoxPluginAuditDirCommand");

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerTemplateInstallationChecker.class);

    public ClouderaManagerTemplateInstallationChecker(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject) throws ApiException {
        CommandsResourceApi commandsResourceApi = clouderaManagerApiPojoFactory.getCommandsResourceApi(pollerObject.getApiClient());
        ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());
        if (apiCommand.isActive()) {
            LOGGER.debug("Command [" + getCommandName() + "] with id [" + pollerObject.getId() + "] is active, so it hasn't finished yet");
            return false;
        } else if (apiCommand.isSuccess()) {
            return true;
        } else {
            fail("", apiCommand, commandsResourceApi, pollerObject.getStack().getType());
        }
        return false;
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        String msg = "Installation of CDP with Cloudera Manager has timed out (command id: " + pollerObject.getId() + ").";
        try {
            CommandsResourceApi commandsResourceApi = clouderaManagerApiPojoFactory.getCommandsResourceApi(pollerObject.getApiClient());
            ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());
            fail(msg, apiCommand, commandsResourceApi, pollerObject.getStack().getType());
        } catch (ApiException e) {
            LOGGER.info("Cloudera Manager had run into a timeout, and we were unable to determine the failure reason", e);
        }
        throw new ClouderaManagerOperationFailedException(msg);
    }

    @Override
    protected String getCommandName() {
        return "Template install";
    }

    private void fail(String messagePrefix, ApiCommand apiCommand, CommandsResourceApi commandsResourceApi, StackType stackType) {
        List<CommandDetails> failedCommands = ClouderaManagerCommandUtil.getFailedOrActiveCommands(apiCommand, commandsResourceApi);
        String msg = messagePrefix + "Installation of CDP with Cloudera Manager has failed. " + CommandDetailsFormatter.formatFailedCommands(failedCommands);
        LOGGER.debug("Top level command {}. Failed or active commands: {}", CommandDetails.fromApiCommand(apiCommand), failedCommands);
        if (stackType == StackType.DATALAKE) {
            for (CommandDetails failedCommand : failedCommands) {
                // Unfortunately CM is not giving back too many details about the errors, and what is returned usually nondeterministic:
                // In a good case it returns "Failed to create HDFS directory",
                // but sometimes it just returns "Aborted command" or "Command timed-out after 186 seconds", so matching on such generic error messages
                // has no added value, therefore we are just checking whether AuditDir related commands are failing or not.
                if (CLOUD_STORAGE_RELATED_COMMANDS.contains(failedCommand.getName())
                        && CommandDetails.CommandStatus.FAILED == failedCommand.getCommandStatus()) {
                    throw new CloudStorageConfigurationFailedException(msg);
                }
            }
        }
        throw new ClouderaManagerOperationFailedException(msg);
    }

}
