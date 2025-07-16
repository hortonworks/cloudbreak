package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_FREEIPA_STATUS_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeFreeIpaStatusValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeFreeIpaStatusValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.freeipa.FreeipaService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeFreeIpaStatusValidationHandler extends ExceptionCatcherEventHandler<ClusterUpgradeFreeIpaStatusValidationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeFreeIpaStatusValidationHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeipaService freeipaService;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeFreeIpaStatusValidationEvent> event) {
        LOGGER.debug("Accepting Cluster upgrade FreeIPA status validation event.");
        ClusterUpgradeFreeIpaStatusValidationEvent request = event.getData();
        Long stackId = request.getResourceId();
        StackView stack = getStack(stackId);
        String environmentCrn = stack.getEnvironmentCrn();
        if (!freeipaService.checkFreeipaRunning(environmentCrn, stack.getName())) {
            String message = "Upgrade cannot be performed because the FreeIPA isn't available. Please check the FreeIPA state and try again.";
            LOGGER.info("FreeIPA status validation failed with: {}", message);
            return new ClusterUpgradeValidationFailureEvent(stackId, new UpgradeValidationFailedException(message));
        } else {
            LOGGER.debug("FreeIPA status validation passed successfully");
            return new ClusterUpgradeFreeIpaStatusValidationFinishedEvent(stackId);
        }
    }

    private StackView getStack(Long stackId) {
        return stackService.getViewByIdWithoutAuth(stackId);
    }

    @Override
    public String selector() {
        return VALIDATE_FREEIPA_STATUS_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeFreeIpaStatusValidationEvent> event) {
        LOGGER.error("Cluster upgrade FreeIPA status validation was unsuccessful due to an unexpected error", e);
        return new ClusterUpgradeValidationFailureEvent(resourceId, e);
    }
}
