package com.sequenceiq.cloudbreak.cm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClouderaManagerTemplateInstallChecker extends AbstractClouderaManagerCommandCheckerTask<ClouderaManagerCommandPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerTemplateInstallChecker.class);

    @Override
    public void handleTimeout(ClouderaManagerCommandPollerObject clouderaManagerPollerObject) {
        throw new ClouderaManagerOperationFailedException("Operation timed out. Template install timed out with this command id: "
                + clouderaManagerPollerObject.getId());
    }

    @Override
    public String successMessage(ClouderaManagerCommandPollerObject clouderaManagerPollerObject) {
        return String.format("Template installation success for stack '%s'", clouderaManagerPollerObject.getStack().getId());
    }

    @Override
    String getCommandName() {
        return "Template install";
    }
}
