package com.sequenceiq.periscope.monitor.handler;

import static com.sequenceiq.periscope.domain.MetricType.IPA_USER_SYNC_FAILED;
import static com.sequenceiq.periscope.domain.MetricType.IPA_USER_SYNC_INVOCATION;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricTag;
import com.sequenceiq.freeipa.api.v1.freeipa.user.UserV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.periscope.service.PeriscopeMetricService;

@Service
public class FreeIpaCommunicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCommunicator.class);

    private final UserV1Endpoint userV1Endpoint;

    @Inject
    private PeriscopeMetricService metricService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory internalCrnGeneratorFactory;

    public FreeIpaCommunicator(UserV1Endpoint userV1Endpoint) {
        this.userV1Endpoint = userV1Endpoint;
    }

    @Retryable(value = Exception.class, maxAttempts = 5, backoff = @Backoff(delay = 10000))
    public SyncOperationStatus synchronizeAllUsers(SynchronizeAllUsersRequest request) {
        LOGGER.info("Invoking freeIpa user sync request: {}", request);
        String envCrn = request.getEnvironments().iterator().next();
        SyncOperationStatus lastSyncStatus = ThreadBasedUserCrnProvider.doAsInternalActor(
                internalCrnGeneratorFactory.autoscale().getInternalCrnForServiceAsString(),
                () -> userV1Endpoint.getLastSyncOperationStatus(envCrn));
        if (SynchronizationStatus.RUNNING.equals(lastSyncStatus.getStatus())) {
            LOGGER.info("There is a user sync operation already running for environment: {} with operationId: {}, " +
                    "skipping request to trigger another user sync", envCrn, lastSyncStatus.getOperationId());
            return lastSyncStatus;
        } else {
            return invokeFreeipaUserSyncAndHandleException(request, envCrn);
        }
    }

    private SyncOperationStatus invokeFreeipaUserSyncAndHandleException(SynchronizeAllUsersRequest request, String envCrn) {
        try {
            SyncOperationStatus status = ThreadBasedUserCrnProvider.doAsInternalActor(
                    internalCrnGeneratorFactory.autoscale().getInternalCrnForServiceAsString(),
                    () -> userV1Endpoint.synchronizeAllUsers(request));
            metricService.incrementMetricCounter(IPA_USER_SYNC_INVOCATION, MetricTag.TENANT.name(), request.getAccountId());
            return status;
        } catch (Exception ex) {
            LOGGER.error("Failed to synchronize users to IPA for environment: {}", envCrn, ex);
            metricService.incrementMetricCounter(IPA_USER_SYNC_FAILED, MetricTag.TENANT.name(), request.getAccountId());
            throw ex;
        }
    }
}
