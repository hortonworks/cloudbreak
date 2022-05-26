package com.sequenceiq.cloudbreak.authdistributor;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UserState;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.opentracing.Tracer;

@Component
public class GrpcAuthDistributorClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcAuthDistributorClient.class);

    @Qualifier("authDistributorManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private Tracer tracer;

    public static GrpcAuthDistributorClient createClient(ManagedChannelWrapper channelWrapper, Tracer tracer,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        GrpcAuthDistributorClient client = new GrpcAuthDistributorClient();
        client.channelWrapper = Preconditions.checkNotNull(channelWrapper, "channelWrapper should not be null.");
        client.tracer = Preconditions.checkNotNull(tracer, "tracer should not be null.");
        client.regionAwareInternalCrnGeneratorFactory = Preconditions.checkNotNull(regionAwareInternalCrnGeneratorFactory,
                "regionAwareInternalCrnGeneratorFactory should not be null.");
        return client;
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 20000))
    public void updateAuthViewForEnvironment(String environmentCrn, UserState userState) {
        LOGGER.debug("Updating auth view for environment: {}", environmentCrn);
        AuthDistributorClient authDistributorClient = makeClient(channelWrapper, tracer, regionAwareInternalCrnGeneratorFactory);
        authDistributorClient.updateAuthViewForEnvironment(MDCBuilder.getOrGenerateRequestId(), environmentCrn, userState);
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 20000))
    public void removeAuthViewForEnvironment(String environmentCrn) {
        LOGGER.debug("Remove auth view for environment: {}", environmentCrn);
        AuthDistributorClient authDistributorClient = makeClient(channelWrapper, tracer, regionAwareInternalCrnGeneratorFactory);
        authDistributorClient.removeAuthViewForEnvironment(MDCBuilder.getOrGenerateRequestId(), environmentCrn);
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 20000))
    public Optional<UserState> fetchAuthViewForEnvironment(String environmentCrn) {
        LOGGER.debug("Fetch auth view for environment: {}", environmentCrn);
        AuthDistributorClient authDistributorClient = makeClient(channelWrapper, tracer, regionAwareInternalCrnGeneratorFactory);
        return authDistributorClient.fetchAuthViewForEnvironment(MDCBuilder.getOrGenerateRequestId(), environmentCrn);
    }

    private AuthDistributorClient makeClient(ManagedChannelWrapper channelWrapper, Tracer tracer,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new AuthDistributorClient(channelWrapper.getChannel(), tracer, regionAwareInternalCrnGeneratorFactory);
    }
}
