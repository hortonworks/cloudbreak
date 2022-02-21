package com.sequenceiq.cloudbreak.usage;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.usage.model.UsageContext;
import com.sequenceiq.cloudbreak.usage.strategy.CompositeUsageProcessingStrategy;
import com.sequenceiq.cloudbreak.usage.strategy.UsageProcessingStrategy;

@Service
public class UsageReportProcessor implements UsageReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsageReportProcessor.class);

    private final UsageProcessingStrategy usageProcessingStrategy;

    public UsageReportProcessor(CompositeUsageProcessingStrategy compositeUsageProcessingStrategy) {
        this.usageProcessingStrategy = compositeUsageProcessingStrategy;
    }

    @Override
    public void cdpDatahubClusterRequested(long timestamp, UsageProto.CDPDatahubClusterRequested details) {
        try {
            checkNotNull(details);
            usageProcessingStrategy.processUsage(eventBuilder()
                    .setTimestamp(timestamp)
                    .setCdpDatahubClusterRequested(details)
                    .build(), UsageContext.Builder.newBuilder()
                    .accountId(details.getAccountId())
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
            usageProcessingStrategy.processUsage(eventBuilder()
                    .setCdpDatahubClusterStatusChanged(details)
                    .build(), null);
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpDatalakeClusterRequested(long timestamp, UsageProto.CDPDatalakeClusterRequested details) {
        try {
            checkNotNull(details);
            usageProcessingStrategy.processUsage(eventBuilder()
                    .setTimestamp(timestamp)
                    .setCdpDatalakeClusterRequested(details)
                    .build(), UsageContext.Builder.newBuilder()
                    .accountId(details.getAccountId())
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
            UsageProto.Event event = eventBuilder()
                    .setCdpDatalakeClusterStatusChanged(details)
                    .build();
            usageProcessingStrategy.processUsage(event, null);
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpEnvironmentRequested(UsageProto.CDPEnvironmentRequested details) {
        try {
            checkNotNull(details);
            usageProcessingStrategy.processUsage(eventBuilder()
                    .setCdpEnvironmentRequested(details)
                    .build(), UsageContext.Builder.newBuilder()
                    .accountId(getAccountId(details.getOperationDetails()))
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
            usageProcessingStrategy.processUsage(eventBuilder()
                    .setCdpEnvironmentStatusChanged(details)
                    .build(), UsageContext.Builder.newBuilder()
                    .accountId(getAccountId(details.getOperationDetails()))
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
            usageProcessingStrategy.processUsage(eventBuilder()
                    .setCdpDatalakeRequested(details)
                    .build(), UsageContext.Builder.newBuilder()
                    .accountId(getAccountId(details.getOperationDetails()))
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
            usageProcessingStrategy.processUsage(eventBuilder()
                    .setCdpDatalakeStatusChanged(details)
                    .build(), UsageContext.Builder.newBuilder()
                    .accountId(getAccountId(details.getOperationDetails()))
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
            usageProcessingStrategy.processUsage(eventBuilder()
                    .setCdpDatahubRequested(details)
                    .build(), UsageContext.Builder.newBuilder()
                    .accountId(getAccountId(details.getOperationDetails()))
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
            usageProcessingStrategy.processUsage(eventBuilder()
                    .setCdpDatahubStatusChanged(details)
                    .build(), UsageContext.Builder.newBuilder()
                    .accountId(getAccountId(details.getOperationDetails()))
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
            usageProcessingStrategy.processUsage(eventBuilder()
                    .setCdpDatalakeSync(details)
                    .build(), UsageContext.Builder.newBuilder()
                    .accountId(getAccountId(details.getOperationDetails()))
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
            usageProcessingStrategy.processUsage(eventBuilder()
                    .setCdpDatahubSync(details)
                    .build(), UsageContext.Builder.newBuilder()
                    .accountId(getAccountId(details.getOperationDetails()))
                    .build());
            LOGGER.info("Logged binary format for the following usage event: {}", details);
        } catch (Exception e) {
            LOGGER.warn("Could not log binary format for the following usage event: {}! Cause: {}", details, e.getMessage());
        }
    }

    @Override
    public void cdpDatahubAutoscaleTriggered(UsageProto.CDPDatahubAutoscaleTriggered details) {
        checkNotNull(details);
        usageProcessingStrategy.processUsage(eventBuilder()
                .setCdpDatahubAutoscaleTriggered(details)
                .build(), null);
    }

    @Override
    public void cdpDatahubAutoscaleConfigChanged(UsageProto.CDPDatahubAutoscaleConfigChanged details) {
        checkNotNull(details);
        usageProcessingStrategy.processUsage(eventBuilder()
                .setCdpDatahubAutoscaleConfigChanged(details)
                .build(), null);
    }

    @Override
    public void cdpNetworkCheckEvent(UsageProto.CDPNetworkCheck details) {
        checkNotNull(details);
        usageProcessingStrategy.processUsage(eventBuilder()
                .setCdpNetworkCheck(details)
                .build(), UsageContext.Builder.newBuilder()
                .accountId(details.getAccountId())
                .build());
    }

    @Override
    public void cdpVmDiagnosticsEvent(UsageProto.CDPVMDiagnosticsEvent details) {
        checkNotNull(details);
        usageProcessingStrategy.processUsage(eventBuilder()
                .setCdpVmDiagnosticsEvent(details)
                .build(), UsageContext.Builder.newBuilder()
                .accountId(details.getAccountId())
                .build());
    }

    private UsageProto.Event.Builder eventBuilder() {
        return UsageProto.Event.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTimestamp(Instant.now().toEpochMilli())
                .setVersion(UsageReporter.USAGE_VERSION);
    }

    private String getAccountId(final UsageProto.CDPOperationDetails operationDetails) {
        return operationDetails != null ? operationDetails.getAccountId() : null;
    }
}
