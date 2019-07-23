package com.sequenceiq.redbeams.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class RedbeamsApiUserCrnClient extends AbstractUserCrnServiceClient<RedbeamsApiUserCrnEndpoint> {
    protected RedbeamsApiUserCrnClient(String serviceAddress, ConfigKey configKey, String apiRoot) {
        super(serviceAddress, configKey, apiRoot);
    }

    @Override
    public RedbeamsApiUserCrnEndpoint withCrn(String crn) {
        return new RedbeamsApiUserCrnEndpoint(getWebTarget(), crn);
    }
}
