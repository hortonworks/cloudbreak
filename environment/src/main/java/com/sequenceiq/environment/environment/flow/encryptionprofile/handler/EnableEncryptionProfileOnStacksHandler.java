package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.ENABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileStateSelectors.FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.DatalakeMultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.EnableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.poller.SdxPollerProvider;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.environment.exception.SdxOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

@Component
public class EnableEncryptionProfileOnStacksHandler extends ExceptionCatcherEventHandler<EnableEncryptionProfileEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnableEncryptionProfileOnStacksHandler.class);

    private static final String LEGACY_ENCRYPTION_PROFILE_NAME = "cdp_default_fips_v1";

    @Value("${env.stack.config.update.polling.maximum.seconds:7200}")
    private Integer maxTime;

    @Value("${env.stack.config.update.sleep.time.seconds:60}")
    private Integer sleepTime;

    private final SdxService sdxService;

    private final DatahubService datahubService;

    private final SdxPollerProvider sdxPollerProvider;

    private final DatalakeMultipleFlowsResultEvaluator datalakeMultipleFlowsResultEvaluator;

    private final DatahubPollerProvider datahubPollerProvider;

    private final MultipleFlowsResultEvaluator datahubMultipleFlowsResultEvaluator;

    protected EnableEncryptionProfileOnStacksHandler(SdxService sdxService, DatahubService datahubService,
            SdxPollerProvider sdxPollerProvider, DatalakeMultipleFlowsResultEvaluator datalakeMultipleFlowsResultEvaluator,
            DatahubPollerProvider datahubPollerProvider, MultipleFlowsResultEvaluator datahubMultipleFlowsResultEvaluator) {
        this.sdxService = sdxService;
        this.datahubService = datahubService;
        this.sdxPollerProvider = sdxPollerProvider;
        this.datalakeMultipleFlowsResultEvaluator = datalakeMultipleFlowsResultEvaluator;
        this.datahubPollerProvider = datahubPollerProvider;
        this.datahubMultipleFlowsResultEvaluator = datahubMultipleFlowsResultEvaluator;
    }

    @Override
    public String selector() {
        return ENABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT.name();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnableEncryptionProfileEvent> event) {
        return new EnableEncryptionProfileFailedEvent(event.getData().getResourceId(), event.getData().getResourceName(),
                event.getData().getResourceCrn(), e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnableEncryptionProfileEvent> event) {
        EnableEncryptionProfileEvent payload = event.getData();
        Long envId = payload.getResourceId();
        String envCrn = payload.getResourceCrn();
        String envName = payload.getResourceName();
        LOGGER.info("Enabling encryption profile on stacks for environment {} (crn={})", envName, envCrn);

        List<FlowIdentifier> datalakeFlowIds = triggerDatalake(envCrn);
        if (!datalakeFlowIds.isEmpty()) {
            waitForDatalakeFlowIds(envId, datalakeFlowIds);
        }

        List<FlowIdentifier> datahubFlowIds = triggerDatahubs(envCrn);
        if (!datahubFlowIds.isEmpty()) {
            waitForDatahubFlowIds(envId, datahubFlowIds);
        }

        LOGGER.info("Encryption profile enabled on all stacks for environment {}", envName);
        return EnableEncryptionProfileEvent.builder()
                .withSelector(FINISH_ENABLE_ENCRYPTION_PROFILE_EVENT.selector())
                .withResourceId(envId)
                .withResourceName(envName)
                .withResourceCrn(envCrn)
                .withEncryptionProfileCrn(payload.getEncryptionProfileCrn())
                .build();
    }

    private List<FlowIdentifier> triggerDatalake(String envCrn) {
        List<SdxClusterResponse> datalakes = sdxService.listByEnvironmentCrn(envCrn);
        List<SdxClusterResponse> runningDatalakes = datalakes.stream()
                .filter(dl -> SdxClusterStatusResponse.RUNNING.equals(dl.getStatus()))
                .toList();
        if (runningDatalakes.isEmpty()) {
            LOGGER.info("No RUNNING datalake found for environment {}, skipping datalake step", envCrn);
            return List.of();
        }
        if (runningDatalakes.size() > 1) {
            LOGGER.warn("Multiple RUNNING datalakes found for environment {} ({}); only the first will be processed", envCrn, runningDatalakes.size());
        }
        SdxClusterResponse dl = runningDatalakes.getFirst();
        if (datalakeHasOwnEncryptionProfile(dl.getCrn())) {
            return List.of();
        }
        LOGGER.info("Triggering enable encryption profile on datalake {} for environment {} "
                + "(env-level profile inherited, no cluster-level CRN forwarded)", dl.getCrn(), envCrn);
        return List.of(sdxService.enableEncryptionProfile(dl.getCrn(), null));
    }

    private List<FlowIdentifier> triggerDatahubs(String envCrn) {
        StackViewV4Responses datahubs = datahubService.list(envCrn);
        if (datahubs.getResponses() == null || datahubs.getResponses().isEmpty()) {
            LOGGER.info("No datahubs found for environment {}, skipping datahub step", envCrn);
            return List.of();
        }
        List<FlowIdentifier> flowIds = datahubs.getResponses().stream()
                .filter(dh -> dh.getCluster() != null && Status.AVAILABLE.equals(dh.getCluster().getStatus()))
                .filter(dh -> !skipDatahub(dh))
                .map(dh -> triggerDatahubSafely(dh, envCrn))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        if (flowIds.isEmpty()) {
            LOGGER.info("No AVAILABLE datahubs eligible for enable encryption profile for environment {}, skipping datahub step", envCrn);
        }
        return flowIds;
    }

    private boolean skipDatahub(StackViewV4Response dh) {
        try {
            return datahubHasOwnEncryptionProfile(dh);
        } catch (Exception e) {
            LOGGER.warn("Failed to inspect encryption profile for datahub {}, skipping it", dh.getCrn(), e);
            return true;
        }
    }

    private Optional<FlowIdentifier> triggerDatahubSafely(StackViewV4Response dh, String envCrn) {
        try {
            LOGGER.info("Triggering enable encryption profile on datahub {} for environment {} "
                    + "(env-level profile inherited, no cluster-level CRN forwarded)", dh.getCrn(), envCrn);
            return Optional.of(datahubService.updateSslConfigs(dh.getCrn(), null));
        } catch (Exception e) {
            LOGGER.warn("Failed to trigger enable-encryption-profile on datahub {}, skipping it", dh.getCrn(), e);
            return Optional.empty();
        }
    }

    private boolean datalakeHasOwnEncryptionProfile(String datalakeCrn) {
        String clusterEncryptionProfileCrn = Optional.ofNullable(sdxService.getDetailByCrn(datalakeCrn))
                .map(SdxClusterDetailResponse::getStackV4Response)
                .map(StackV4Response::getCluster)
                .map(ClusterV4Response::getEncryptionProfileCrn)
                .orElse(null);
        if (hasOwnEncryptionProfile(clusterEncryptionProfileCrn)) {
            LOGGER.info("Skipping datalake {} — it has its own encryption profile [{}]", datalakeCrn, clusterEncryptionProfileCrn);
            return true;
        }
        return false;
    }

    private boolean datahubHasOwnEncryptionProfile(StackViewV4Response datahub) {
        String datahubCrn = datahub.getCrn();
        String clusterEncryptionProfileCrn = Optional.ofNullable(datahubService.getByCrn(datahubCrn, Collections.emptySet()))
                .map(StackV4Response::getCluster)
                .map(ClusterV4Response::getEncryptionProfileCrn)
                .orElse(null);
        if (hasOwnEncryptionProfile(clusterEncryptionProfileCrn)) {
            LOGGER.info("Skipping datahub {} — it has its own encryption profile [{}]", datahubCrn, clusterEncryptionProfileCrn);
            return true;
        }
        return false;
    }

    private boolean hasOwnEncryptionProfile(String encryptionProfileCrn) {
        if (StringUtils.isBlank(encryptionProfileCrn)) {
            return false;
        }
        Crn crn = Crn.safeFromString(encryptionProfileCrn);
        return !LEGACY_ENCRYPTION_PROFILE_NAME.equals(crn.getResource());
    }

    private void waitForDatalakeFlowIds(Long envId, List<FlowIdentifier> flowIdentifiers) {
        try {
            Polling.stopAfterDelay(maxTime, TimeUnit.SECONDS)
                    .stopIfException(true)
                    .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                    .run(sdxPollerProvider.flowListPoller(envId, flowIdentifiers));
            if (datalakeMultipleFlowsResultEvaluator.anyFailed(flowIdentifiers)) {
                throw new SdxOperationFailedException(
                        String.format("Failed to enable encryption profile on datalake. Flow ids: %s", flowIdentifiers));
            }
        } catch (PollerStoppedException e) {
            LOGGER.warn("Enable encryption profile on datalake stacks timed out or error happened.", e);
            throw new CloudbreakServiceException(
                    String.format("Enable encryption profile on datalake stacks timed out or error happened: %s", e.getMessage()), e);
        }
    }

    private void waitForDatahubFlowIds(Long envId, List<FlowIdentifier> flowIdentifiers) {
        try {
            Polling.stopAfterDelay(maxTime, TimeUnit.SECONDS)
                    .stopIfException(true)
                    .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                    .run(datahubPollerProvider.multipleFlowsPoller(envId, flowIdentifiers));
            List<FlowIdentifier> failedFlows = datahubMultipleFlowsResultEvaluator.collectFailed(flowIdentifiers);
            if (!failedFlows.isEmpty()) {
                String message = String.format("Failed to enable encryption profile on %d attached datahub(s). Failed flow ids: %s",
                        failedFlows.size(), failedFlows);
                throw new DatahubOperationFailedException(message);
            }
        } catch (PollerStoppedException e) {
            LOGGER.warn("Enable encryption profile on datahub stacks timed out or error happened.", e);
            throw new CloudbreakServiceException(
                    String.format("Enable encryption profile on datahub stacks timed out or error happened: %s", e.getMessage()), e);
        }
    }
}
