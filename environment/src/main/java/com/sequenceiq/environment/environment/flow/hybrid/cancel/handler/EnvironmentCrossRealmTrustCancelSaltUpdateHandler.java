package com.sequenceiq.environment.environment.flow.hybrid.cancel.handler;

import static com.sequenceiq.environment.environment.EnvironmentStatus.TRUST_CANCEL_SALT_UPDATE_FAILED;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelHandlerSelectors.TRUST_CANCEL_SALT_UPDATE_HANDLER;
import static com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelStateSelectors.FINISH_TRUST_CANCEL_CONFIG_REMOVAL_EVENT;

import java.util.List;
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
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelEvent;
import com.sequenceiq.environment.environment.flow.hybrid.cancel.event.EnvironmentCrossRealmTrustCancelFailedEvent;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.service.stack.StackPollerService;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

/**
 * Runs a salt update on all DataHub stacks after the FreeIPA trust entity has been deleted.
 * At this point KerberosPillarConfigGenerator finds no trust info and returns an empty trust pillar,
 * which causes the file.absent Salt state to remove /etc/krb5.conf.d/trust.conf from every cluster node.
 */
@Component
public class EnvironmentCrossRealmTrustCancelSaltUpdateHandler extends ExceptionCatcherEventHandler<EnvironmentCrossRealmTrustCancelEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentCrossRealmTrustCancelSaltUpdateHandler.class);

    @Value("${env.saltupdate.datahub.polling.attempt:45}")
    private Integer attempt;

    @Value("${env.saltupdate.datahub.polling.sleep.time:20}")
    private Integer sleeptime;

    private final StackPollerService stackPollerService;

    private final DatahubPollerProvider datahubPollerProvider;

    private final MultipleFlowsResultEvaluator multipleFlowsResultEvaluator;

    protected EnvironmentCrossRealmTrustCancelSaltUpdateHandler(
            StackPollerService stackPollerService,
            DatahubPollerProvider datahubPollerProvider,
            MultipleFlowsResultEvaluator multipleFlowsResultEvaluator) {
        this.stackPollerService = stackPollerService;
        this.datahubPollerProvider = datahubPollerProvider;
        this.multipleFlowsResultEvaluator = multipleFlowsResultEvaluator;
    }

    @Override
    public String selector() {
        return TRUST_CANCEL_SALT_UPDATE_HANDLER.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvironmentCrossRealmTrustCancelEvent> event) {
        return new EnvironmentCrossRealmTrustCancelFailedEvent(event.getData(), e, TRUST_CANCEL_SALT_UPDATE_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvironmentCrossRealmTrustCancelEvent> event) {
        LOGGER.debug("In EnvironmentCrossRealmTrustCancelSaltUpdateHandler.accept");
        EnvironmentCrossRealmTrustCancelEvent data = event.getData();
        try {
            LOGGER.info("Cross Realm Trust cancel: running salt update on stacks to remove trust.conf for environment: {}", data.getResourceCrn());
            List<FlowIdentifier> flowIdentifiers = stackPollerService.updateSaltOnStacks(data.getResourceId(), data.getResourceCrn());
            waitOnFlowIds(data.getResourceId(), flowIdentifiers);
            LOGGER.debug("FINISH_TRUST_CANCEL_CONFIG_REMOVAL_EVENT event sent");
            return EnvironmentCrossRealmTrustCancelEvent.builder()
                    .withSelector(FINISH_TRUST_CANCEL_CONFIG_REMOVAL_EVENT.selector())
                    .withResourceCrn(data.getResourceCrn())
                    .withResourceId(data.getResourceId())
                    .withResourceName(data.getResourceName())
                    .build();
        } catch (Exception e) {
            LOGGER.error("TRUST_CANCEL_SALT_UPDATE_FAILED event sent", e);
            return new EnvironmentCrossRealmTrustCancelFailedEvent(data, e, TRUST_CANCEL_SALT_UPDATE_FAILED);
        }
    }

    private void waitOnFlowIds(Long envId, List<FlowIdentifier> saltUpdateFlows) {
        if (CollectionUtils.isEmpty(saltUpdateFlows)) {
            return;
        }
        try {
            Polling.stopAfterAttempt(attempt)
                    .stopIfException(true)
                    .waitPeriodly(sleeptime, TimeUnit.SECONDS)
                    .run(datahubPollerProvider.multipleFlowsPoller(envId, saltUpdateFlows));
            List<FlowIdentifier> failed = multipleFlowsResultEvaluator.collectFailed(saltUpdateFlows);
            LOGGER.warn("Finished waiting for salt update on stacks, flows that failed: [{}]", failed);
            int failedFlowCount = failed.size();
            if (failedFlowCount > 0) {
                String message = String.format("Salt update failed on %d attached clusters.", failedFlowCount);
                throw new DatahubOperationFailedException(message);
            }
        } catch (PollerStoppedException e) {
            LOGGER.warn("Salt update on stacks timed out or error happened.", e);
            throw new DatahubOperationFailedException("Salt update on stacks timed out or error happened: " + e.getMessage(), e);
        }
    }
}

