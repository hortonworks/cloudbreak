package com.sequenceiq.cloudbreak.authdistributor;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UserState;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.authdistributor.config.AuthDistributorConfig;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

@Component
public class GrpcAuthDistributorClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcAuthDistributorClient.class);

    @Qualifier("authDistributorManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private AuthDistributorConfig authDistributorConfig;

    public static GrpcAuthDistributorClient createClient(ManagedChannelWrapper channelWrapper, AuthDistributorConfig authDistributorConfig,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        GrpcAuthDistributorClient client = new GrpcAuthDistributorClient();
        client.channelWrapper = Preconditions.checkNotNull(channelWrapper, "channelWrapper should not be null.");
        client.authDistributorConfig = Preconditions.checkNotNull(authDistributorConfig, "authDistributorConfig should not be null.");
        client.regionAwareInternalCrnGeneratorFactory = Preconditions.checkNotNull(regionAwareInternalCrnGeneratorFactory,
                "regionAwareInternalCrnGeneratorFactory should not be null.");
        return client;
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 20000))
    public void updateAuthViewForEnvironment(String environmentCrn, UserState userState) {
        LOGGER.debug("Updating auth view for environment: {}", environmentCrn);
        AuthDistributorClient authDistributorClient = makeClient(channelWrapper, regionAwareInternalCrnGeneratorFactory);
        authDistributorClient.updateAuthViewForEnvironment(environmentCrn, userState);
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 20000))
    public void removeAuthViewForEnvironment(String environmentCrn) {
        LOGGER.debug("Remove auth view for environment: {}", environmentCrn);
        AuthDistributorClient authDistributorClient = makeClient(channelWrapper, regionAwareInternalCrnGeneratorFactory);
        authDistributorClient.removeAuthViewForEnvironment(environmentCrn);
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 20000))
    public Optional<UserState> fetchAuthViewForEnvironment(String environmentCrn) {
        LOGGER.debug("Fetch auth view for environment: {}", environmentCrn);
        AuthDistributorClient authDistributorClient = makeClient(channelWrapper, regionAwareInternalCrnGeneratorFactory);
        return authDistributorClient.fetchAuthViewForEnvironment(environmentCrn);
    }

    private AuthDistributorClient makeClient(ManagedChannelWrapper channelWrapper,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new AuthDistributorClient(channelWrapper.getChannel(), authDistributorConfig, regionAwareInternalCrnGeneratorFactory);
    }
}
