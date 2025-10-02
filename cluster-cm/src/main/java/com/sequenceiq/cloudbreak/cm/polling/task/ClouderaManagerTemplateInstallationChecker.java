package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cluster.service.ClusterEventService;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.error.mapper.ClouderaManagerErrorMapperService;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetails;
import com.sequenceiq.cloudbreak.cm.exception.CommandDetailsFormatter;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.cm.util.ClouderaManagerCommandUtil;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;

public class ClouderaManagerTemplateInstallationChecker extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Set<String> CLOUD_STORAGE_RELATED_COMMANDS = Set.of(
            "RangerPluginCreateAuditDir",
            "CreateRangerAuditDir",
            "CreateRangerKafkaPluginAuditDirCommand",
            "CreateHiveWarehouseExternalDir",
            "CreateHiveWarehouseDir",
            "CreateRangerKnoxPluginAuditDirCommand");

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerTemplateInstallationChecker.class);

    private ClouderaManagerErrorMapperService clouderaManagerErrorMapperService;

    public ClouderaManagerTemplateInstallationChecker(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            ClusterEventService clusterEventService,
            ClouderaManagerErrorMapperService clouderaManagerErrorMapperService) {
        super(clouderaManagerApiPojoFactory, clusterEventService);
        this.clouderaManagerErrorMapperService = clouderaManagerErrorMapperService;
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
            fail("", apiCommand, commandsResourceApi, pollerObject.getStack());
        }
        return false;
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        String msg = "Installation of CDP with Cloudera Manager has timed out (command id: " + pollerObject.getId() + ").";
        try {
            CommandsResourceApi commandsResourceApi = clouderaManagerApiPojoFactory.getCommandsResourceApi(pollerObject.getApiClient());
            ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());
            fail(msg, apiCommand, commandsResourceApi, pollerObject.getStack());
        } catch (ApiException e) {
            LOGGER.info("Cloudera Manager had run into a timeout, and we were unable to determine the failure reason", e);
        }
        throw new ClouderaManagerOperationFailedException(msg);
    }

    @Override
    protected String getCommandName() {
        return "Template install";
    }

    private void fail(String messagePrefix, ApiCommand apiCommand, CommandsResourceApi commandsResourceApi, StackDtoDelegate stack) {
        List<CommandDetails> failedCommands = ClouderaManagerCommandUtil.getFailedOrActiveCommands(apiCommand, commandsResourceApi);
        String msg = messagePrefix + "Installation of CDP with Cloudera Manager has failed. " + CommandDetailsFormatter.formatFailedCommands(failedCommands);
        LOGGER.debug("Top level command {}. Failed or active commands: {}", CommandDetails.fromApiCommand(apiCommand), failedCommands);
        throw new ClouderaManagerOperationFailedException(clouderaManagerErrorMapperService.map(stack, failedCommands, msg));
    }

}
