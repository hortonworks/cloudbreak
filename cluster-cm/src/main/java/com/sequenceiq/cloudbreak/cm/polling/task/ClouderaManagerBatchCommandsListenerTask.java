package com.sequenceiq.cloudbreak.cm.polling.task;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandListPollerObject;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerBatchCommandsListenerTask extends AbstractClouderaManagerCommandListCheckerTask<ClouderaManagerCommandListPollerObject> {
    private String commandName;

    public ClouderaManagerBatchCommandsListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService, String commandName) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
        this.commandName = commandName;
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandListPollerObject pollerObject) {
        throw new ClouderaManagerOperationFailedException(String.format("Operation timed out. Failed to execute all of the following commands: %s.",
                pollerObject.getIdList()));
    }

    @Override
    public String successMessage(ClouderaManagerCommandListPollerObject pollerObject) {
        return String.format("Cloudera Manager executed the following commands: %s", pollerObject.getIdList());
    }

    @Override
    protected String getCommandName() {
        return "Batch command of " + commandName + " commands";
    }
}
