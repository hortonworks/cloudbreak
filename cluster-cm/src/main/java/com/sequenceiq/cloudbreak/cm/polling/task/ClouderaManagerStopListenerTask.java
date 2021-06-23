package com.sequenceiq.cloudbreak.cm.polling.task;

import java.util.Optional;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerStopListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    public ClouderaManagerStopListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
    }

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject pollerObject) {
        getCloudbreakEventService().fireClusterManagerEvent(pollerObject.getStack().getId(), pollerObject.getStack().getStatus().name(),
                ResourceEvent.CLUSTER_CM_COMMAND_TIMEOUT, Optional.of(pollerObject.getId()));
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to stop Cloudera Manager services.");
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject toolsResourceApi) {
        return "All services have been stopped in Cloudera Manager successfully.";
    }

    @Override
    protected String getCommandName() {
        return "Stop";
    }
}
