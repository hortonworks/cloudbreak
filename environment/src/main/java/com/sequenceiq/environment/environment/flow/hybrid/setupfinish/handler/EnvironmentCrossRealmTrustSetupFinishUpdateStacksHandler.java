package com.sequenceiq.environment.environment.flow.hybrid.setupfinish.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FINISH_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishHandlerSelectors.SETUP_FINISH_TRUST_UPDATE_STACKS_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.FINISH_TRUST_SETUP_FINISH_EVENT;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateTrustedRealmRequest;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoBase;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishFailedEvent;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.cluster.ClusterService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class EnvironmentCrossRealmTrustSetupFinishUpdateStacksHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustSetupFinishEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustSetupFinishUpdateStacksHandler.class);

    @Value("${env.saltupdate.datahub.polling.attempt:45}")
    private Integer attempt;

    @Value("${env.saltupdate.datahub.polling.sleep.time:20}")
    private Integer sleeptime;

    private final StackV4Endpoint stackV4Endpoint;

    private final DatahubPollerProvider datahubPollerProvider;

    private final MultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    private final EnvironmentService environmentService;

    private final ClusterService clusterService;

    private final FreeIpaService freeIpaService;

    protected EnvironmentCrossRealmTrustSetupFinishUpdateStacksHandler(StackV4Endpoint stackV4Endpoint, DatahubPollerProvider datahubPollerProvider,
            MultipleFlowsResultEvaluator multipleFlowsResultEvaluator, EnvironmentService environmentService, ClusterService clusterService,
            FreeIpaService freeIpaService) {
        this.stackV4Endpoint = stackV4Endpoint;
        this.datahubPollerProvider = datahubPollerProvider;
        this.multipleFlowsResultEvaluator = multipleFlowsResultEvaluator;
        this.environmentService = environmentService;
        this.clusterService = clusterService;
        this.freeIpaService = freeIpaService;
    }

    @Override
    public String selector() {
        return SETUP_FINISH_TRUST_UPDATE_STACKS_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustSetupFinishEvent> event) {
        return new EnvironmentCrossRealmTrustSetupFinishFailedEvent(event.getData(), e, TRUST_SETUP_FINISH_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustSetupFinishEvent> event) {
        LOGGER.debug("In EnvironmentCrossRealmTrustSetupFinishUpdateStacksHandler.accept");
        EnvironmentCrossRealmTrustSetupFinishEvent data = event.getData();
        try {
            Optional<EnvironmentDto> environmentDto = environmentService.findById(data.getResourceId());
            EnvironmentType environmentType = environmentDto.map(EnvironmentDtoBase::getEnvironmentType).orElse(EnvironmentType.PUBLIC_CLOUD);
            String envCrn = environmentDto.map(EnvironmentDtoBase::getResourceCrn).orElse(data.getResourceCrn());
            boolean saltUpdateRequired = environmentType == EnvironmentType.PUBLIC_CLOUD;

            String realm = freeIpaService.describe(envCrn)
                    .map(response -> response.getTrust().getRealm().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new CloudbreakServiceException("Failed to get realm from FreeIPA for environment " + envCrn));

            List<String> stackCrns = clusterService.getStackCrnsForConfigUpdate(envCrn, environmentType);
            LOGGER.info("Triggering update trusted realm chain flow for {} stacks (saltUpdateRequired={}, realm={})",
                    stackCrns.size(), saltUpdateRequired, realm);

            List<FlowIdentifier> flowIdentifiers = stackCrns.stream()
                    .map(crn -> {
                        UpdateTrustedRealmRequest request = new UpdateTrustedRealmRequest();
                        request.setRealm(realm);
                        request.setSaltUpdateRequired(saltUpdateRequired);
                        return ThreadBasedUserCrnProvider.doAsInternalActor(() -> stackV4Endpoint.triggerUpdateTrustedRealm(0L, crn, request));
                    })
                    .collect(Collectors.toList());

            waitForFlowIds(data.getResourceId(), flowIdentifiers);

            return EnvironmentCrossRealmTrustSetupFinishEvent.builder()
                    .withSelector(FINISH_TRUST_SETUP_FINISH_EVENT.selector())
                    .withResourceCrn(data.getResourceCrn())
                    .withResourceId(data.getResourceId())
                    .withResourceName(data.getResourceName())
                    .build();
        } catch (Exception e) {
            return new EnvironmentCrossRealmTrustSetupFinishFailedEvent(data, e, TRUST_SETUP_FINISH_FAILED);
        }
    }

    private void waitForFlowIds(Long envId, List<FlowIdentifier> flowIdentifiers) {
        if (flowIdentifiers.isEmpty()) {
            LOGGER.info("No stacks to update, skipping polling.");
            return;
        }
        try {
            Polling.stopAfterAttempt(attempt)
                    .stopIfException(true)
                    .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                    .run(datahubPollerProvider.multipleFlowsPoller(envId, flowIdentifiers));
            int failedFlowCount = multipleFlowsResultEvaluator.collectFailed(flowIdentifiers).size();
            if (failedFlowCount > 0) {
                String message = String.format("Failed to update trusted realm on %d attached clusters.", failedFlowCount);
                throw new DatahubOperationFailedException(message);
            }
        } catch (PollerStoppedException e) {
            LOGGER.warn("Update trusted realm on stacks timed out or error happened.", e);
            throw new DatahubOperationFailedException("Update trusted realm on stacks timed out or error happened: " + e.getMessage());
        }
    }
}
