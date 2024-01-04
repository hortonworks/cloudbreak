package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_SERVICES_EVENT;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeServiceValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFinishedEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.validation.service.ServiceUpgradeValidationRequest;
import com.sequenceiq.cloudbreak.service.upgrade.validation.service.ServiceUpgradeValidator;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeServiceValidationHandler extends ExceptionCatcherEventHandler<ClusterUpgradeServiceValidationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeServiceValidationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private List<ServiceUpgradeValidator> serviceUpgradeValidators;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeServiceValidationEvent> event) {
        ClusterUpgradeServiceValidationEvent request = event.getData();
        LOGGER.debug("Accepting Cluster upgrade service validation event. {}", request);
        Long stackId = request.getResourceId();
        try {
            ServiceUpgradeValidationRequest validationRequest = createValidationRequest(request, stackId);
            LOGGER.debug("Running the following upgrade validations: {}", serviceUpgradeValidators);
            serviceUpgradeValidators.forEach(validator -> validator.validate(validationRequest));
            return new ClusterUpgradeValidationFinishedEvent(stackId);
        } catch (UpgradeValidationFailedException e) {
            LOGGER.warn("Cluster upgrade service validation failed", e);
            return new ClusterUpgradeValidationFailureEvent(stackId, e);
        } catch (Exception e) {
            LOGGER.error("Cluster upgrade service validation was unsuccessful due to an internal error", e);
            return new ClusterUpgradeValidationFinishedEvent(stackId, e);
        }
    }

    private ServiceUpgradeValidationRequest createValidationRequest(ClusterUpgradeServiceValidationEvent request, Long stackId) {
        StackDto stack = stackDtoService.getById(stackId);
        return new ServiceUpgradeValidationRequest(stack, request.isLockComponents(), request.isRollingUpgradeEnabled(), request.getTargetRuntime(),
                request.getUpgradeImageInfo());
    }

    @Override
    public String selector() {
        return VALIDATE_SERVICES_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeServiceValidationEvent> event) {
        LOGGER.error("Cluster upgrade service validation was unsuccessful due to an unexpected error", e);
        return new ClusterUpgradeValidationFinishedEvent(resourceId, e);
    }
}
