package com.sequenceiq.redbeams.client;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class RedbeamsServiceCrnClient extends AbstractUserCrnServiceClient<RedbeamsServiceCrnEndpoints> {

    protected RedbeamsServiceCrnClient(String serviceAddress, ConfigKey configKey, String apiRoot) {
        super(serviceAddress, configKey, apiRoot);
    }

    @Override
    public RedbeamsServiceCrnEndpoints withCrn(String crn) {
        return new RedbeamsServiceCrnEndpoints(getWebTarget(), crn);
    }
}
