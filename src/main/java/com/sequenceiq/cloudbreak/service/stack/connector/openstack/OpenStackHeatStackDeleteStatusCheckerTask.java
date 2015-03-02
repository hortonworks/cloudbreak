package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import org.springframework.stereotype.Component;

@Component
public class OpenStackHeatStackDeleteStatusCheckerTask extends OpenStackHeatStackStatusCheckerTask {

    @Override
    public boolean exitPolling(OpenStackContext t) {
        return false;
    }
}
