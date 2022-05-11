package com.sequenceiq.cloudbreak.authdistributor;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UserState;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.authdistributor.config.AuthDistributorConfig;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

import io.grpc.ManagedChannel;
import io.opentracing.Tracer;

@Component
public class GrpcAuthDistributorClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcAuthDistributorClient.class);

    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private AuthDistributorConfig authDistributorConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private Tracer tracer;

    private AuthDistributorClient makeClient(ManagedChannel channel, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new AuthDistributorClient(channel, authDistributorConfig, tracer, regionAwareInternalCrnGeneratorFactory);
    }

    public void updateAuthViewForEnvironment(String environmentCrn, UserState userState) {
        LOGGER.debug("Updating auth view for environment: {}", environmentCrn);
        AuthDistributorClient authDistributorClient = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        authDistributorClient.updateAuthViewForEnvironment(MDCBuilder.getOrGenerateRequestId(), environmentCrn, userState);
        LOGGER.debug("Auth view for environment: {} has been updated.", environmentCrn);
    }

    public void removeAuthViewForEnvironment(String environmentCrn) {
        LOGGER.debug("Remove auth view for environment: {}", environmentCrn);
        AuthDistributorClient authDistributorClient = makeClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
        authDistributorClient.removeAuthViewForEnvironment(MDCBuilder.getOrGenerateRequestId(), environmentCrn);
        LOGGER.debug("Auth view for environment: {} has been removed.", environmentCrn);
    }
}
