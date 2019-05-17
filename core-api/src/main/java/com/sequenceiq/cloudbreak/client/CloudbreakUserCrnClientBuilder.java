package com.sequenceiq.cloudbreak.client;

import com.sequenceiq.cloudbreak.api.CoreApi;

public class CloudbreakUserCrnClientBuilder extends AbstractUserCrnServiceClientBuilder {
    public CloudbreakUserCrnClientBuilder(String cloudbreakAddress) {
        super(cloudbreakAddress);
    }

    @Override
    protected CloudbreakUserCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new CloudbreakUserCrnClient(serviceAddress, configKey, CoreApi.API_ROOT_CONTEXT);
    }
}
