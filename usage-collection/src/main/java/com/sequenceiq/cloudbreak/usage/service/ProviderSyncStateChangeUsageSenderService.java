package com.sequenceiq.cloudbreak.usage.service;

import java.time.Instant;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPProviderSyncStateChange;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.common.model.ProviderSyncState;

@Service
public class ProviderSyncStateChangeUsageSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderSyncStateChangeUsageSenderService.class);

    @Inject
    private UsageReporter usageReporter;

    public void addProviderSyncState(ProviderSyncState syncState, String resourceCrn) {
        sendUsageReport(syncState, resourceCrn, null, UsageProto.CDPProviderSyncStateOperation.Value.ADDED);
    }

    public void removeProviderSyncState(ProviderSyncState syncState, String resourceCrn) {
        sendUsageReport(syncState, resourceCrn, null, UsageProto.CDPProviderSyncStateOperation.Value.REMOVED);
    }

    private void sendUsageReport(
            ProviderSyncState syncState,
            String resourceCrn,
            String reason,
            UsageProto.CDPProviderSyncStateOperation.Value status) {
        try {
            LOGGER.debug("Send cdp provider sync state usage report for syncState: {}, status: {}, reason: {}", syncState, status, reason);
            usageReporter.cdpProviderSyncStateChange(CDPProviderSyncStateChange.newBuilder()
                    .setAccountId(Crn.safeFromString(resourceCrn).getAccountId())
                    .setSyncState(syncState.name())
                    .setTimestamp(Instant.now().toEpochMilli())
                    .setResourceCrn(resourceCrn)
                    .setReason(reason == null ? "" : reason)
                    .setOperation(status)
                    .build());
        } catch (Exception e) {
            LOGGER.error("Couldn't send usage report about cdp provider sync state change: {}, status: {}", syncState, status, e);
        }
    }
}
