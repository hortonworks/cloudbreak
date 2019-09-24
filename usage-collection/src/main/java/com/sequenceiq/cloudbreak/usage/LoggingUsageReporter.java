package com.sequenceiq.cloudbreak.usage;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

/**
 * A usage reporter that logs usage events.
 */
public class LoggingUsageReporter implements UsageReporter {
    private static final Logger BINARY_EVENT_LOGGER = LoggerFactory.getLogger("CDP_BINARY_USAGE_EVENT");

    private static final String USAGE_EVENT_MDC_NAME = "binaryUsageEvent";

    @Override
    public void cdpDatahubClusterRequested(long timestamp, UsageProto.CDPDatahubClusterRequested details) {
        checkNotNull(details);
        log(eventBuilder()
                .setTimestamp(timestamp)
                .setCdpDatahubClusterRequested(details)
                .build());
    }

    @Override
    public void cdpDatahubClusterStatusChanged(UsageProto.CDPDatahubClusterStatusChanged details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpDatahubClusterStatusChanged(details)
                .build());
    }

    @Override
    public void cdpDatalakeClusterRequested(long timestamp, UsageProto.CDPDatalakeClusterRequested details) {
        checkNotNull(details);
        log(eventBuilder()
                .setTimestamp(timestamp)
                .setCdpDatalakeClusterRequested(details)
                .build());
    }

    @Override
    public void cdpDatalakeClusterStatusChanged(UsageProto.CDPDatalakeClusterStatusChanged details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpDatalakeClusterStatusChanged(details)
                .build());
    }

    private UsageProto.Event.Builder eventBuilder() {
        return UsageProto.Event.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTimestamp(Instant.now().toEpochMilli())
                .setVersion(UsageReporter.USAGE_VERSION);
    }

    void log(UsageProto.Event event) {
        String binaryUsageEvent = BaseEncoding.base64().encode(event.toByteArray());
        MDCBuilder.addMdcField(USAGE_EVENT_MDC_NAME, binaryUsageEvent);
        BINARY_EVENT_LOGGER.info(binaryUsageEvent);
        MDCBuilder.removeMdcField(USAGE_EVENT_MDC_NAME);
    }
}
