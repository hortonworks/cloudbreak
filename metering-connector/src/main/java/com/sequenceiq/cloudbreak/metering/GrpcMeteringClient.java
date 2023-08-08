package com.sequenceiq.cloudbreak.metering;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;

@Component
public class GrpcMeteringClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcMeteringClient.class);

    @Qualifier("meteringManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public static GrpcMeteringClient createClient(ManagedChannelWrapper channelWrapper,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        GrpcMeteringClient client = new GrpcMeteringClient();
        client.channelWrapper = Preconditions.checkNotNull(channelWrapper, "channelWrapper should not be null.");
        client.regionAwareInternalCrnGeneratorFactory = Preconditions.checkNotNull(regionAwareInternalCrnGeneratorFactory,
                "regionAwareInternalCrnGeneratorFactory should not be null.");
        return client;
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 20000))
    public void sendMeteringEvent(MeteringEvent meteringEvent) {
        LOGGER.debug("Send metering event for environment: {}", meteringEvent.getEnvironmentCrn());
        MeteringClient meteringClient = makeClient(channelWrapper, regionAwareInternalCrnGeneratorFactory);
        meteringClient.sendMeteringEvent(meteringEvent);
    }

    private MeteringClient makeClient(ManagedChannelWrapper channelWrapper,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        return new MeteringClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory);
    }
}