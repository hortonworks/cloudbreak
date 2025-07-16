package com.sequenceiq.environment.environment.service.freeipa;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.environment.environment.poller.FreeIpaPollerProvider;
import com.sequenceiq.environment.exception.FreeIpaOperationFailedException;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.VerticalScaleResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;

@Service
public class FreeIpaPollerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaPollerService.class);

    @Value("${env.stop.polling.attempt:180}")
    private Integer startStopAttempt;

    @Value("${env.upgradeccm.freeipa.polling.attempt:60}")
    private Integer upgradeccmAttempt;

    @Value("${env.modifyproxy.freeipa.polling.attempt:60}")
    private Integer modifyProxyAttempt;

    @Value("${env.verticalscale.freeipa.polling.attempt:60}")
    private Integer verticalscaleAttempt;

    @Value("${env.crossrealm.freeipa.polling.attempt:30}")
    private Integer crossRealmAttempt;

    @Value("${env.stop.polling.sleep.time:10}")
    private Integer startStopSleeptime;

    @Value("${env.upgradeccm.freeipa.polling.sleeptime:20}")
    private Integer upgradeccmSleeptime;

    @Value("${env.modifyproxy.freeipa.polling.sleeptime:20}")
    private Integer modifyProxySleeptime;

    @Value("${env.verticalscale.freeipa.polling.sleeptime:30}")
    private Integer verticalscaleSleeptime;

    @Value("${env.crossrealm.freeipa.polling.sleeptime:10}")
    private Integer crossRealmSleeptime;

    private final FreeIpaService freeIpaService;

    private final FreeIpaPollerProvider freeipaPollerProvider;

    public FreeIpaPollerService(FreeIpaService freeIpaService, FreeIpaPollerProvider freeipaPollerProvider) {
        this.freeIpaService = freeIpaService;
        this.freeipaPollerProvider = freeipaPollerProvider;
    }

    public void startAttachedFreeipaInstances(Long envId, String envCrn) {
        executeFreeIpaOperationAndStartPolling(envCrn, freeIpaService::startFreeIpa, freeipaPollerProvider.startPoller(envId, envCrn), Status::isStartable);
    }

    public void stopAttachedFreeipaInstances(Long envId, String envCrn) {
        executeFreeIpaOperationAndStartPolling(envCrn, freeIpaService::stopFreeIpa, freeipaPollerProvider.stopPoller(envId, envCrn), Status::isStoppable);
    }

    public void waitForSynchronizeUsers(Long envId, String envCrn) {
        executeFreeIpaSyncOperationAndStartPolling(envCrn, freeIpaService::synchronizeAllUsersInEnvironment,
                opId -> freeipaPollerProvider.syncUsersPoller(envId, envCrn, opId), AvailabilityStatus::isAvailable);
    }

    public void waitForCcmUpgrade(Long envId, String envCrn) {
        OperationStatus status = freeIpaService.upgradeCcm(envCrn);
        if (status.getStatus() != OperationState.COMPLETED) {
            try {
                Polling.stopAfterAttempt(upgradeccmAttempt)
                        .stopIfException(true)
                        .waitPeriodly(upgradeccmSleeptime, TimeUnit.SECONDS)
                        .run(() -> freeipaPollerProvider.upgradeCcmPoller(envId, envCrn, status.getOperationId()));
            } catch (PollerStoppedException e) {
                LOGGER.warn("FreeIPA Upgrade CCM timed out or error happened.", e);
                throw new FreeIpaOperationFailedException("FreeIPA upgrade of Cluster Connectivity Manager timed out or error happened: " + e.getMessage());
            }
        }
    }

    public void waitForModifyProxyConfig(Long envId, String envCrn, String previousProxyCrn) {
        OperationStatus status = freeIpaService.modifyProxyConfig(envCrn, previousProxyCrn);
        if (status.getStatus() != OperationState.COMPLETED) {
            try {
                Polling.stopAfterAttempt(modifyProxyAttempt)
                        .stopIfException(true)
                        .waitPeriodly(modifyProxySleeptime, TimeUnit.SECONDS)
                        .run(() -> freeipaPollerProvider.modifyProxyConfigPoller(envId, envCrn, status.getOperationId()));
            } catch (PollerStoppedException e) {
                LOGGER.warn("FreeIPA modify proxy config timed out or error happened.", e);
                throw new FreeIpaOperationFailedException("FreeIPA proxy config modification timed out or error happened: " + e.getMessage());
            }
        }
    }

    public void waitForVerticalScale(Long envId, String envCrn, VerticalScaleRequest freeIPAVerticalScaleRequest) {
        VerticalScaleResponse response = freeIpaService.verticalScale(envCrn, freeIPAVerticalScaleRequest);
        if (response.getFlowIdentifier() != null) {
            try {
                Polling.stopAfterAttempt(verticalscaleAttempt)
                        .stopIfException(true)
                        .waitPeriodly(verticalscaleSleeptime, TimeUnit.SECONDS)
                        .run(() -> freeipaPollerProvider.verticalScalePoller(envId, envCrn, response.getFlowIdentifier().getPollableId()));
            } catch (PollerStoppedException e) {
                LOGGER.warn("FreeIPA Vertical Scale timed out or error happened.", e);
                throw new FreeIpaOperationFailedException("FreeIPA Vertical Scale timed out or error happened: " + e.getMessage());
            }
        }
    }

    public void waitForCrossRealmTrustSetup(Long envId, String envCrn, PrepareCrossRealmTrustRequest prepareCrossRealmTrustRequest) {
        PrepareCrossRealmTrustResponse response = freeIpaService.crossRealmPrepare(envCrn, prepareCrossRealmTrustRequest);
        if (response.getFlowIdentifier() != null) {
            try {
                Polling.stopAfterAttempt(crossRealmAttempt)
                        .stopIfException(true)
                        .waitPeriodly(crossRealmSleeptime, TimeUnit.SECONDS)
                        .run(() -> freeipaPollerProvider.crossRealmFlowCheck(envId, envCrn, response.getFlowIdentifier().getPollableId()));
            } catch (PollerStoppedException e) {
                LOGGER.warn("FreeIPA cross realm trust prepare timed out or error happened.", e);
                throw new FreeIpaOperationFailedException("FreeIPAcross realm trust prepare timed out or error happened: " + e.getMessage());
            }
        }
    }

    public void waitForCrossRealmFinish(Long envId, String envCrn, FinishSetupCrossRealmTrustRequest finishCrossRealmTrustRequest) {
        FinishSetupCrossRealmTrustResponse response = freeIpaService.crossRealmFinish(envCrn, finishCrossRealmTrustRequest);
        if (response.getFlowIdentifier() != null) {
            try {
                Polling.stopAfterAttempt(crossRealmAttempt)
                        .stopIfException(true)
                        .waitPeriodly(crossRealmSleeptime, TimeUnit.SECONDS)
                        .run(() -> freeipaPollerProvider.crossRealmFlowCheck(envId, envCrn, response.getFlowIdentifier().getPollableId()));
            } catch (PollerStoppedException e) {
                LOGGER.warn("FreeIPA cross realm trust finish timed out or error happened.", e);
                throw new FreeIpaOperationFailedException("FreeIPAcross realm trust finish timed out or error happened: " + e.getMessage());
            }
        }
    }

    public void waitForEnableSeLinux(Long envId, String envCrn) {
        // TODO implement once we have the freeipa flow
    }

    private void executeFreeIpaOperationAndStartPolling(String envCrn, Consumer<String> freeIpaOperation, AttemptMaker<Void> attemptMaker,
            Function<Status, Boolean> shouldRun) {
        Optional<DescribeFreeIpaResponse> freeIpaResponse = freeIpaService.describe(envCrn);
        if (freeIpaResponse.isPresent() && shouldRun.apply(freeIpaResponse.get().getStatus())) {
            freeIpaOperation.accept(envCrn);
            try {
                LOGGER.info("FreeIpa polling started.");
                Polling.stopAfterAttempt(startStopAttempt)
                        .stopIfException(true)
                        .waitPeriodly(startStopSleeptime, TimeUnit.SECONDS)
                        .run(attemptMaker);
            } catch (PollerStoppedException e) {
                LOGGER.warn("Error while sending resource definition request", e);
                throw new FreeIpaOperationFailedException("FreeIPA operation is timed out", e);
            }
        }
    }

    private void executeFreeIpaSyncOperationAndStartPolling(String envCrn, Function<String, SyncOperationStatus> freeIpaSyncOperation,
            Function<String, AttemptMaker<Void>> attemptMaker, Function<AvailabilityStatus, Boolean> shouldRun) {

        Optional<DescribeFreeIpaResponse> freeIpaResponse = freeIpaService.describe(envCrn);
        if (freeIpaResponse.isPresent() && freeIpaResponse.get().getAvailabilityStatus() != null &&
                shouldRun.apply(freeIpaResponse.get().getAvailabilityStatus())) {
            SyncOperationStatus status = freeIpaSyncOperation.apply(envCrn);
            try {
                Polling.stopAfterAttempt(startStopAttempt)
                        .stopIfException(true)
                        .waitPeriodly(startStopSleeptime, TimeUnit.SECONDS)
                        .run(attemptMaker.apply(status.getOperationId()));
            } catch (PollerStoppedException e) {
                LOGGER.warn("FreeIPA syncing timed out", e);
                throw new FreeIpaOperationFailedException("FreeIPA syncing timed out");
            }
        }
    }
}
