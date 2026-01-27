package com.sequenceiq.environment.environment.flow.hybrid.setupfinish.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_SETUP_FINISH_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishHandlerSelectors.SETUP_FINISH_TRUST_UPDATE_STACKS_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishStateSelectors.FINISH_TRUST_SETUP_FINISH_EVENT;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.dyngr.Polling;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.EnvironmentType;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoBase;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishEvent;
import com.sequenceiq.environment.environment.flow.hybrid.setupfinish.event.EnvironmentCrossRealmTrustSetupFinishFailedEvent;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.stack.StackPollerService;
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

    private final StackPollerService stackPollerService;

    private final DatahubPollerProvider datahubPollerProvider;

    private final MultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    private final EnvironmentService environmentService;

    protected EnvironmentCrossRealmTrustSetupFinishUpdateStacksHandler(StackPollerService stackPollerService,
            DatahubPollerProvider datahubPollerProvider, MultipleFlowsResultEvaluator multipleFlowsResultEvaluator, EnvironmentService environmentService) {
        this.stackPollerService = stackPollerService;
        this.datahubPollerProvider = datahubPollerProvider;
        this.multipleFlowsResultEvaluator = multipleFlowsResultEvaluator;
        this.environmentService = environmentService;
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
            if (environmentType == EnvironmentType.PUBLIC_CLOUD) {
                LOGGER.info("FreeIPA Cross Realm Trust setup finished. Updating Kerberos on the stacks in public cloud environment.");
                List<FlowIdentifier> flowIdentifiers = stackPollerService.updateSaltOnStacks(data.getResourceId(), data.getResourceCrn());
                waitForSaltUpdateOnFlowIds(data.getResourceId(), flowIdentifiers);
            }
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

    private void waitForSaltUpdateOnFlowIds(Long envId, List<FlowIdentifier> saltUpdateFlows) {
        try {
            Polling.stopAfterAttempt(attempt)
                    .stopIfException(true)
                    .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                    .run(datahubPollerProvider.multipleFlowsPoller(envId, saltUpdateFlows));
            int failedFlowCount = multipleFlowsResultEvaluator.collectFailed(saltUpdateFlows).size();
            if (failedFlowCount > 0) {
                String message = String.format("Failed to update salt state on %d attached clusters.", failedFlowCount);
                throw new DatahubOperationFailedException(message);
            }
        } catch (PollerStoppedException e) {
            LOGGER.warn("Salt update on stacks timed out or error happened.", e);
            throw new DatahubOperationFailedException("Salt update on stacks timed out or error happened: " + e.getMessage());
        }
    }
}
