package com.sequenceiq.cloudbreak.cm.polling.task;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerUpgradeRuntimeListenerTask extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerPollerObject> {

    public ClouderaManagerUpgradeRuntimeListenerTask(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject toolsResourceApi) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Failed to upgrade Runtime services.");
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject toolsResourceApi) {
        return "Successfully upgraded CDP Runtime services.";
    }

    @Override
    protected String getCommandName() {
        return "Upgrade CDP Runtime services";
    }
}
