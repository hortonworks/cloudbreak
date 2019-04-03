package com.sequenceiq.cloudbreak.cm.polling.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cm.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerCommandPollerObject;

@Service
public class ClouderaManagerParcelRepoChecker extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerParcelRepoChecker.class);

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject clouderaManagerPollerObject) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Parcel repo sync timed out with this command id: "
                + clouderaManagerPollerObject.getId());
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject clouderaManagerPollerObject) {
        return String.format("Parcel repo sync success for stack '%s'", clouderaManagerPollerObject.getStack().getId());
    }

    @Override
    protected String getCommandName() {
        return "Parcel repo sync";
    }
}
