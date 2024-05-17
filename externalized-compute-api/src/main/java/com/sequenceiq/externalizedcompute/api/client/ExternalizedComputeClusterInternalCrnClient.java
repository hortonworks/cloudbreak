package com.sequenceiq.externalizedcompute.api.client;


import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;

public class ExternalizedComputeClusterInternalCrnClient extends AbstractUserCrnServiceClient<ExternalizedComputeClusterCrnEndpoint> {

    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    public ExternalizedComputeClusterInternalCrnClient(String serviceAddress, ConfigKey configKey, String apiRoot,
            RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator) {
        super(serviceAddress, configKey, apiRoot);
        this.regionAwareInternalCrnGenerator = regionAwareInternalCrnGenerator;
    }

    public ExternalizedComputeClusterCrnEndpoint withInternalCrn() {
        return new ExternalizedComputeClusterCrnEndpoint(getWebTarget(), regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString());
    }

    public ExternalizedComputeClusterCrnEndpoint withCrn(String userCrn) {
        return new ExternalizedComputeClusterCrnEndpoint(getWebTarget(), userCrn);
    }
}