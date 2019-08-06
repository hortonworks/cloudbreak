package com.sequenceiq.redbeams.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClientBuilder;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.redbeams.api.RedbeamsApi;

public class RedbeamsServiceClientBuilder extends AbstractUserCrnServiceClientBuilder<RedbeamsServiceCrnClient> {

    public RedbeamsServiceClientBuilder(String serviceAddress) {
        super(serviceAddress);
    }

    @Override
    protected RedbeamsServiceCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new RedbeamsServiceCrnClient(serviceAddress, configKey, RedbeamsApi.API_ROOT_CONTEXT);
    }
}
