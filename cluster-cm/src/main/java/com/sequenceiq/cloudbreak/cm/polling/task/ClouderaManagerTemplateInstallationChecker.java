package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.exception.CloudStorageConfigurationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.cm.util.ClouderaManagerCommandApiErrorParserUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerTemplateInstallationChecker extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerTemplateInstallationChecker.class);

    public ClouderaManagerTemplateInstallationChecker(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
    }

    @Override
    protected boolean doStatusCheck(ClouderaManagerCommandPollerObject pollerObject, CommandsResourceApi commandsResourceApi) throws ApiException {
        commandsResourceApi = clouderaManagerApiPojoFactory.getCommandsResourceApi(pollerObject.getApiClient());
        ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());
        if (apiCommand.getActive()) {
            LOGGER.debug("Command [" + getCommandName() + "] with id [" + pollerObject.getId() + "] is active, so it hasn't finished yet");
            return false;
        } else if (apiCommand.getSuccess()) {
            return true;
        } else {
            fail("", apiCommand, commandsResourceApi);
        }
        return false;
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        String msg = "Installation of CDP with Cloudera Manager has timed out (command id: " + pollerObject.getId() + ").";
        try {
            CommandsResourceApi commandsResourceApi = clouderaManagerApiPojoFactory.getCommandsResourceApi(pollerObject.getApiClient());
            ApiCommand apiCommand = commandsResourceApi.readCommand(pollerObject.getId());
            fail(msg, apiCommand, commandsResourceApi);
        } catch (ApiException e) {
            LOGGER.info("Cloudera Manager had run into a timeout, and we were unable to determine the failure reason", e);
        }

        throw new ClouderaManagerOperationFailedException(msg);
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject) {
        return String.format("Template installation success for stack '%s'", clouderaManagerCommandPollerObject.getStack().getId());
    }

    @Override
    protected String getCommandName() {
        return "Template install";
    }

    private void fail(String messagePrefix, ApiCommand apiCommand, CommandsResourceApi commandsResourceApi) {
        List<String> errorReasons = ClouderaManagerCommandApiErrorParserUtil.getErrors(apiCommand, commandsResourceApi);
        String msg = messagePrefix + "Installation of CDP with Cloudera Manager has failed: [" + String.join(", ", errorReasons) + "]";
        LOGGER.info(msg);
        for (String errorReason : errorReasons) {
            // Unfortunately CM is not giving back too much details about the errors, and what is returned usually nondeterministic:
            // In a good case it returns "Failed to create HDFS directory",
            // but sometines it just returns "Aborted command" or "Command timed-out after 186 seconds", so matching on such generic error error messages
            // has no added value, therefore we are just checing whether AuditDir related commands are failing or not.
            if (errorReason.matches(".*[RangerPluginCreateAuditDir|CreateRangerAuditDir|CreateRangerKafkaPluginAuditDirCommand|" +
                    "CreateHiveWarehouseExternalDir|CreateHiveWarehouseDir|CreateRangerKnoxPluginAuditDirCommand].*")) {
                throw new CloudStorageConfigurationFailedException(msg);
            }
        }
        throw new ClouderaManagerOperationFailedException(msg);
    }
}
