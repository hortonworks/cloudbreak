package com.sequenceiq.cloudbreak.metering;

import static com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventRequest;
import static com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionProto.SubmitEventResponse;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import static org.glassfish.jersey.internal.guava.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionGrpc;
import com.cloudera.thunderhead.service.meteringingestion.MeteringIngestionGrpc.MeteringIngestionBlockingStub;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.grpc.altus.AltusMetadataInterceptor;
import com.sequenceiq.cloudbreak.grpc.util.GrpcUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;

import io.grpc.ManagedChannel;

public class MeteringClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringClient.class);

    private final ManagedChannel channel;

    private final RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private final MeteringConfig meteringConfig;

    public MeteringClient(ManagedChannel channel, RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory,
            MeteringConfig meteringConfig) {
        this.channel = channel;
        this.regionAwareInternalCrnGeneratorFactory = regionAwareInternalCrnGeneratorFactory;
        this.meteringConfig = meteringConfig;
    }

    public SubmitEventResponse sendMeteringEvent(MeteringEvent meteringEvent) {
        checkNotNull(meteringEvent, "meteringEvent should not be null.");
        SubmitEventRequest request = SubmitEventRequest.newBuilder()
                .setEvent(meteringEvent)
                .build();
        return newStub().submitEvent(request);
    }

    private MeteringIngestionBlockingStub newStub() {
        String requestId = MDCBuilder.getOrGenerateRequestId();
        return MeteringIngestionGrpc.newBlockingStub(channel)
                .withInterceptors(
                        GrpcUtil.getTimeoutInterceptor(meteringConfig.getGrpcTimeoutInSeconds()),
                        new AltusMetadataInterceptor(requestId, regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString())
                );
    }
}