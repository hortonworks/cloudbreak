package com.sequenceiq.cloudbreak.client;

import com.sequenceiq.cloudbreak.api.CoreApi;

public class CloudbreakUserCrnClientBuilder extends AbstractUserCrnServiceClientBuilder<CloudbreakServiceUserCrnClient> {
    public CloudbreakUserCrnClientBuilder(String cloudbreakAddress) {
        super(cloudbreakAddress);
    }

    @Override
    protected CloudbreakServiceUserCrnClient createUserCrnClient(String serviceAddress, ConfigKey configKey) {
        return new CloudbreakServiceUserCrnClient(serviceAddress, configKey, CoreApi.API_ROOT_CONTEXT);
    }
}