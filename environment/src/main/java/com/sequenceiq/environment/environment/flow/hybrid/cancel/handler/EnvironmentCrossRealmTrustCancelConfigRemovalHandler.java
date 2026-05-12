package com.sequenceiq.environment.environment.flow.hybrid.cancel.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_CONFIG_REMOVAL_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelHandlerSelectors.TRUST_CANCEL_CONFIG_REMOVAL_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.TRUST_CANCEL_TRUST_ENTITY_DELETE_EVENT;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.dyngr.Polling;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelEvent;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelFailedEvent;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.cluster.ClusterService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustResponse;

@Component
public class EnvironmentCrossRealmTrustCancelConfigRemovalHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustCancelEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustCancelConfigRemovalHandler.class);

    @Value("${env.saltupdate.datahub.polling.attempt:45}")
    private Integer attempt;

    @Value("${env.saltupdate.datahub.polling.sleep.time:20}")
    private Integer sleeptime;

    private final DatahubPollerProvider datahubPollerProvider;

    private final MultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    private final ClusterService clusterService;

    private final EnvironmentService environmentService;

    private final FreeIpaService freeIpaService;

    protected EnvironmentCrossRealmTrustCancelConfigRemovalHandler(
            DatahubPollerProvider datahubPollerProvider,
            MultipleFlowsResultEvaluator multipleFlowsResultEvaluator,
            ClusterService clusterService,
            EnvironmentService environmentService,
            FreeIpaService freeIpaService) {
        this.datahubPollerProvider = datahubPollerProvider;
        this.multipleFlowsResultEvaluator = multipleFlowsResultEvaluator;
        this.clusterService = clusterService;
        this.environmentService = environmentService;
        this.freeIpaService = freeIpaService;
    }

    @Override
    public String selector() {
        return TRUST_CANCEL_CONFIG_REMOVAL_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustCancelEvent> event) {
        return new EnvironmentCrossRealmTrustCancelFailedEvent(event.getData(), e, TRUST_CANCEL_CONFIG_REMOVAL_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustCancelEvent> event) {
        LOGGER.debug("In EnvironmentCrossRealmTrustCancelConfigRemovalHandler.accept");
        EnvironmentCrossRealmTrustCancelEvent data = event.getData();
        try {
            Optional<EnvironmentDto> environmentDto = environmentService.findById(data.getResourceId());
            // Fetch the realm here, before the trust entity is deleted, so that ClusterService doesn't need
            // to call FreeIPA itself. On a retry where FreeIPA cancel already ran, the realm may still be
            // carried on the incoming event from the first attempt; otherwise we fetch it fresh from FreeIPA.
            Optional<String> realm = resolveRealm(data);
            EnvironmentCrossRealmTrustCancelEvent.Builder cancelEventBuilder = EnvironmentCrossRealmTrustCancelEvent.builder()
                    .withSelector(TRUST_CANCEL_TRUST_ENTITY_DELETE_EVENT.selector())
                    .withResourceCrn(data.getResourceCrn())
                    .withResourceId(data.getResourceId())
                    .withResourceName(data.getResourceName());
            if (environmentDto.isPresent()) {
                LOGGER.info("Removing trusted realm '{}' from Cloudera Manager on clusters for environment: {}", realm, data.getResourceCrn());
                List<FlowIdentifier> removeTrustedRealmFlows = clusterService.removeTrustedRealmConfigFromClusters(environmentDto, realm.get());
                waitOnFlowIds(data.getResourceId(), removeTrustedRealmFlows);
                cancelEventBuilder.withRealm(realm.get());
            } else {
                LOGGER.warn("Failed to get realm from FreeIPA for environment {}. " +
                        "FreeIPA trust info is unavailable — the trust may have already been fully cancelled on a previous attempt.", data.getResourceCrn());
            }
            LOGGER.debug("TRUST_CANCEL_TRUST_ENTITY_DELETE_EVENT event sent");
            return cancelEventBuilder.build();
        } catch (Exception e) {
            LOGGER.error("TRUST_CANCEL_CONFIG_REMOVAL_FAILED event sent", e);
            return new EnvironmentCrossRealmTrustCancelFailedEvent(data, e, TRUST_CANCEL_CONFIG_REMOVAL_FAILED);
        }
    }

    private Optional<String> resolveRealm(EnvironmentCrossRealmTrustCancelEvent data) {
        if (data.getRealm() != null) {
            LOGGER.debug("Using realm '{}' carried from previous flow step for environment: {}", data.getRealm(), data.getResourceCrn());
            return Optional.of(data.getRealm());
        }
        return freeIpaService.describe(data.getResourceCrn())
                .map(DescribeFreeIpaResponse::getTrust)
                .map(TrustResponse::getRealm)
                .map(realm -> realm.toUpperCase(Locale.ROOT));
    }

    private void waitOnFlowIds(Long envId, List<FlowIdentifier> saltUpdateFlows) {
        if (CollectionUtils.isEmpty(saltUpdateFlows)) {
            return;
        }
        final String topic = "Remove trusted realm config from Cloudera Manager";
        try {
            Polling.stopAfterAttempt(attempt)
                    .stopIfException(true)
                    .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                    .run(datahubPollerProvider.multipleFlowsPoller(envId, saltUpdateFlows));
            List<FlowIdentifier> failed = multipleFlowsResultEvaluator.collectFailed(saltUpdateFlows);
            LOGGER.warn("Finished waiting for {} on stacks, flows that failed: [{}]", topic, failed);
            int failedFlowCount = failed.size();
            if (failedFlowCount > 0) {
                String message = String.format("%s failed on %d attached clusters.", topic, failedFlowCount);
                throw new DatahubOperationFailedException(message);
            }
        } catch (PollerStoppedException e) {
            LOGGER.warn("{} on stacks timed out or error happened.", topic, e);
            throw new DatahubOperationFailedException(topic + " on stacks timed out or error happened: " + e.getMessage(), e);
        }
    }
}
