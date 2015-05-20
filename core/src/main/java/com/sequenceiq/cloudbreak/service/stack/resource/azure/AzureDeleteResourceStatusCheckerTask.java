package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import org.springframework.stereotype.Component;

@Component
public class AzureDeleteResourceStatusCheckerTask extends AzureResourceStatusCheckerTask {

    @Override
    public boolean exitPolling(AzureResourcePollerObject azureResourcePollerObject) {
        return false;
    }

}
