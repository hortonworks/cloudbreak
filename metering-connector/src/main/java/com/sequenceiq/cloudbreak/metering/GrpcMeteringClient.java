package com.sequenceiq.cloudbreak.metering;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;

@Component
public class GrpcMeteringClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcMeteringClient.class);

    @Qualifier("meteringManagedChannelWrapper")
    @Inject
    private ManagedChannelWrapper channelWrapper;

    @Inject
    private MeteringConfig meteringConfig;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private MeteringInfoProvider meteringInfoProvider;

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
        if (meteringConfig.isEnabled()) {
            LOGGER.debug("Send metering event: {}", meteringInfoProvider.getReducedInfo(meteringEvent));
            MeteringClient meteringClient = makeClient();
            meteringClient.sendMeteringEvent(meteringEvent);
        } else {
            LOGGER.debug("Metering event sending is disabled!");
        }
    }

    public void sendMeteringEventWithoutRetry(MeteringEvent meteringEvent) {
        sendMeteringEvent(meteringEvent);
    }

    @VisibleForTesting
    MeteringClient makeClient() {
        return new MeteringClient(channelWrapper.getChannel(), regionAwareInternalCrnGeneratorFactory, meteringConfig);
    }
}