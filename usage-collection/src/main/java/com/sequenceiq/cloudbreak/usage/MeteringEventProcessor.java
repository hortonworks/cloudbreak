package com.sequenceiq.cloudbreak.usage;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.metering.events.MeteringEventsProto;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequest;
import com.sequenceiq.cloudbreak.sigmadbus.model.DatabusRequestContext;
import com.sequenceiq.cloudbreak.usage.metering.MeteringDatabusRecordProcessor;

@Service
public class MeteringEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringEventProcessor.class);

    private static final int METERING_EVENT_VERSION = 3;

    private final MeteringDatabusRecordProcessor meteringDatabusRecordProcessor;

    private final RegionAwareInternalCrnGeneratorFactory internalCrnGeneratorFactory;

    public MeteringEventProcessor(MeteringDatabusRecordProcessor meteringDatabusRecordProcessor,
            RegionAwareInternalCrnGeneratorFactory internalCrnGeneratorFactory) {
        this.meteringDatabusRecordProcessor = meteringDatabusRecordProcessor;
        this.internalCrnGeneratorFactory = internalCrnGeneratorFactory;
    }

    private void processEvent(MeteringEventsProto.MeteringEvent event) {
        DatabusRequestContext dbusContext = DatabusRequestContext.Builder.newBuilder()
                .withAccountId(getAccountId())
                .build();
        DatabusRequest databusRequest = DatabusRequest.Builder.newBuilder()
                .withMessageBody(event)
                .withContext(dbusContext)
                .build();
        meteringDatabusRecordProcessor.processRecord(databusRequest);
    }

    public void storageHeartbeat(MeteringEventsProto.StorageHeartbeat details,
        MeteringEventsProto.ServiceType.Value serviceType,
        MeteringEventsProto.ServiceFeature.Value serviceFeature) {
        try {
            checkNotNull(details);
            checkNotNull(serviceType);
            MeteringEventsProto.MeteringEvent event = eventBuilder()
                    .setStorageHeartbeat(details)
                    .setServiceType(serviceType)
                    .setServiceConfiguration(MeteringEventsProto.ServiceConfiguration.newBuilder()
                            .setServiceFeature(serviceFeature).build())
                    .build();
            processEvent(event);
            LOGGER.info("Sent binary format for the following metering event: {}", details);
        } catch (Exception e) {
            LOGGER.error("Could not send binary format for the following metering event: {}! Cause: {}", details, e.getMessage(), e);
            throw e;
        }
    }

    private MeteringEventsProto.MeteringEvent.Builder eventBuilder() {
        return MeteringEventsProto.MeteringEvent.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTimestamp(Instant.now().toEpochMilli())
                .setVersion(METERING_EVENT_VERSION);
    }

    private String getAccountId() {
        return Crn.safeFromString(internalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()).getAccountId();
    }
}
