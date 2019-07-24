package com.sequenceiq.cloudbreak.usage;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.common.io.BaseEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class LoggingUsageReporter implements UsageReporter {
    private static final Logger BINARY_EVENT_LOGGER = LoggerFactory.getLogger("CDP_BINARY_USAGE_EVENT");

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

    private void log(UsageProto.Event event) {
        BINARY_EVENT_LOGGER.info(BaseEncoding.base64().encode(event.toByteArray()));
    }
}
