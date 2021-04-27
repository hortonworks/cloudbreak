package com.sequenceiq.cloudbreak.cm.polling.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.api.swagger.CommandsResourceApi;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerCollectDiagnosticsListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerCollectDiagnosticsListenerTask.class);

    public ClouderaManagerCollectDiagnosticsListenerTask(ClouderaManagerApiPojoFactory apiPojoFactory, CloudbreakEventService eventService) {
        super(apiPojoFactory, eventService);
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
            String detailedErrorMessage = getResultMessageWithDetailedErrorsPostFix(apiCommand, commandsResourceApi);
            String msg = "Collect diagnostics failed: " + detailedErrorMessage;
            LOGGER.info(msg);
            throw new ClouderaManagerOperationFailedException(msg);
        }
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to collect diagnostics.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject clouderaManagerCommandPollerObject) {
        return "Cloudera Manager diagnostics collection finished with success result.";
    }

    @Override
    protected String getCommandName() {
        return "Collect diagnostics";
    }
}
