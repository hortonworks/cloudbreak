package com.sequenceiq.cloudbreak.service.stack.resource.azure;

import org.springframework.stereotype.Component;

@Component
// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class AzureDeleteResourceStatusCheckerTask extends AzureResourceStatusCheckerTask {

    @Override
    public boolean exitPolling(AzureResourcePollerObject azureResourcePollerObject) {
        return false;
    }

}
