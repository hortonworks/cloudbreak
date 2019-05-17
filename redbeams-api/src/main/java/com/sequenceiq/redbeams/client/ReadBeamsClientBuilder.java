package com.sequenceiq.redbeams.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class ReadBeamsClientBuilder extends AbstractUserCrnServiceClientBuilder {

    public ReadBeamsClientBuilder(String redbeamsAddress) {
        super(redbeamsAddress);
    }

    @Override
    protected RedbeamsClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new RedbeamsClient(serviceAddress, configKey);
    }

}
