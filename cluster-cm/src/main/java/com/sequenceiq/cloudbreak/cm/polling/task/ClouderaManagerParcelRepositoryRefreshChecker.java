package com.sequenceiq.cloudbreak.cm.polling.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.client.ClouderaManagerApiPojoFactory;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollerObject;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

public class ClouderaManagerParcelRepositoryRefreshChecker extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelRepositoryRefreshChecker.class);

    public ClouderaManagerParcelRepositoryRefreshChecker(ClouderaManagerApiPojoFactory clouderaManagerApiPojoFactory,
            CloudbreakEventService cloudbreakEventService) {
        super(clouderaManagerApiPojoFactory, cloudbreakEventService);
    }

    @Override
    public void handleTimeout(ClouderaManagerPollerObject clouderaManagerPollerObject) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Parcel repo sync timed out with this command id: "
                + clouderaManagerPollerObject.getId());
    }

    @Override
    public String successMessage(ClouderaManagerPollerObject clouderaManagerPollerObject) {
        return String.format("Parcel repo sync success for stack '%s'", clouderaManagerPollerObject.getStack().getId());
    }

    @Override
    protected String getCommandName() {
        return "Parcel repo sync";
    }
}
