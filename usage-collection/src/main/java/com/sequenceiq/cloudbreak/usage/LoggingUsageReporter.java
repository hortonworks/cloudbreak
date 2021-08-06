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

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUsageReporter.class);

    private static final Logger BINARY_EVENT_LOGGER = LoggerFactory.getLogger("CDP_BINARY_USAGE_EVENT");

    private static final String USAGE_EVENT_MDC_NAME = "binaryUsageEvent";

    @Override
    public void cdpDatahubClusterRequested(long timestamp, UsageProto.CDPDatahubClusterRequested details) {
        try {
            checkNotNull(details);
            log(eventBuilder()
                    .setTimestamp(timestamp)
                    .setCdpDatahubClusterRequested(details)
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpDatahubClusterStatusChanged(UsageProto.CDPDatahubClusterStatusChanged details) {
        try {
            checkNotNull(details);
            log(eventBuilder()
                    .setCdpDatahubClusterStatusChanged(details)
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpDatalakeClusterRequested(long timestamp, UsageProto.CDPDatalakeClusterRequested details) {
        try {
            checkNotNull(details);
            log(eventBuilder()
                    .setTimestamp(timestamp)
                    .setCdpDatalakeClusterRequested(details)
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpDatalakeClusterStatusChanged(UsageProto.CDPDatalakeClusterStatusChanged details) {
        try {
            checkNotNull(details);
            log(eventBuilder()
                    .setCdpDatalakeClusterStatusChanged(details)
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpEnvironmentRequested(UsageProto.CDPEnvironmentRequested details) {
        try {
            checkNotNull(details);
            log(eventBuilder()
                    .setCdpEnvironmentRequested(details)
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpEnvironmentStatusChanged(UsageProto.CDPEnvironmentStatusChanged details) {
        try {
            checkNotNull(details);
            log(eventBuilder()
                    .setCdpEnvironmentStatusChanged(details)
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpDatalakeRequested(UsageProto.CDPDatalakeRequested details) {
        try {
            checkNotNull(details);
            log(eventBuilder()
                    .setCdpDatalakeRequested(details)
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpDatalakeStatusChanged(UsageProto.CDPDatalakeStatusChanged details) {
        try {
            checkNotNull(details);
            log(eventBuilder()
                    .setCdpDatalakeStatusChanged(details)
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpDatahubRequested(UsageProto.CDPDatahubRequested details) {
        try {
            checkNotNull(details);
            log(eventBuilder()
                    .setCdpDatahubRequested(details)
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpDatahubStatusChanged(UsageProto.CDPDatahubStatusChanged details) {
        try {
            checkNotNull(details);
            log(eventBuilder()
                    .setCdpDatahubStatusChanged(details)
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpDatalakeSync(UsageProto.CDPDatalakeSync details) {
        try {
            checkNotNull(details);
            log(eventBuilder()
                    .setCdpDatalakeSync(details)
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpDatahubSync(UsageProto.CDPDatahubSync details) {
        try {
            checkNotNull(details);
            log(eventBuilder()
                    .setCdpDatahubSync(details)
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    private UsageProto.Event.Builder eventBuilder() {
        return UsageProto.Event.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTimestamp(Instant.now().toEpochMilli())
                .setVersion(UsageReporter.USAGE_VERSION);
    }

    void log(UsageProto.Event event) {
        LOGGER.info("Logging binary format for the following usage event: {}", event);
        String binaryUsageEvent = BaseEncoding.base64().encode(event.toByteArray());
        MDCBuilder.addMdcField(USAGE_EVENT_MDC_NAME, binaryUsageEvent);
        BINARY_EVENT_LOGGER.info(binaryUsageEvent);
        MDCBuilder.removeMdcField(USAGE_EVENT_MDC_NAME);
    }
}
