package com.sequenceiq.cloudbreak.usage;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

/**
 * A usage reporter that logs usage events.
 */
@Service
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

    @Override
    public void cdpEnvironmentRequested(UsageProto.CDPEnvironmentRequested details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpEnvironmentRequested(details)
                .build());
    }

    @Override
    public void cdpEnvironmentStatusChanged(UsageProto.CDPEnvironmentStatusChanged details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpEnvironmentStatusChanged(details)
                .build());
    }

    @Override
    public void cdpDatalakeRequested(UsageProto.CDPDatalakeRequested details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpDatalakeRequested(details)
                .build());
    }

    @Override
    public void cdpDatalakeStatusChanged(UsageProto.CDPDatalakeStatusChanged details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpDatalakeStatusChanged(details)
                .build());
    }

    @Override
    public void cdpDatahubRequested(UsageProto.CDPDatahubRequested details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpDatahubRequested(details)
                .build());
    }

    @Override
    public void cdpDatahubStatusChanged(UsageProto.CDPDatahubStatusChanged details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpDatahubStatusChanged(details)
                .build());
    }

    @Override
    public void cdpDatalakeSync(UsageProto.CDPDatalakeSync details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpDatalakeSync(details)
                .build());
    }

    @Override
    public void cdpDatahubSync(UsageProto.CDPDatahubSync details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpDatahubSync(details)
                .build());
    }

    @Override
    public void cdpDatahubAutoscaleTriggered(UsageProto.CDPDatahubAutoscaleTriggered details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpDatahubAutoscaleTriggered(details)
                .build());
    }

    @Override
    public void cdpDatahubAutoscaleConfigChanged(UsageProto.CDPDatahubAutoscaleConfigChanged details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpDatahubAutoscaleConfigChanged(details)
                .build());
    }

    @Override
    public void cdpNetworkCheckEvent(UsageProto.CDPNetworkCheck details) {
        checkNotNull(details);
        log(eventBuilder()
                .setCdpNetworkCheck(details)
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
