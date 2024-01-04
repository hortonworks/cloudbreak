package com.sequenceiq.cloudbreak.wiam.client;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.workloadiam.WorkloadIamProto.SyncUsersResponse;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

@Component
public class GrpcWiamClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcWiamClient.class);

    @Value("${wiam.grpc.timeout.sec:60}")
    private long grpcTimeoutSec;

    @Qualifier("wiamManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public void syncUsersInEnvironment(String accountId, String environmentCrn, String requestId) {
        WiamClient wiamClient = createClient();
        LOGGER.debug("Initiating usersync for [{}]", environmentCrn);
        SyncUsersResponse response = wiamClient.syncUsersInEnvironment(accountId, environmentCrn,
                Optional.ofNullable(requestId).orElseGet(() -> UUID.randomUUID().toString()));
        LOGGER.debug("Usersync initated with [{}] for [{}]", response.getUsersyncCrn(), response.getEnvironmentCrn());
    }

    private WiamClient createClient() {
        return new WiamClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(), grpcTimeoutSec);
    }
}
