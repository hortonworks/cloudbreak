package com.sequenceiq.cloudbreak.client;

public class CloudbreakServiceUserCrnClient extends AbstractUserCrnServiceClient<CloudbreakServiceCrnEndpoints> {
    public CloudbreakServiceUserCrnClient(String serviceAddress, ConfigKey configKey, String apiRoot) {
        super(serviceAddress, configKey, apiRoot);
    }

    @Override
    public CloudbreakServiceCrnEndpoints withCrn(String crn) {
        return new CloudbreakServiceCrnEndpoints(getWebTarget(), crn);
    }
}
