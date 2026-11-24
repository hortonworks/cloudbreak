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
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.cloudbreak.service.upgrade.validation.service.LockedComponentsValidator;
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
    private LockedComponentsValidator lockedComponentsValidator;

    @Inject
    private List<ServiceUpgradeValidator> serviceUpgradeValidators;

    @Inject
    private ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeServiceValidationEvent> event) {
        ClusterUpgradeServiceValidationEvent request = event.getData();
        LOGGER.debug("Accepting Cluster upgrade service validation event. {}", request);
        Long stackId = request.getResourceId();
        ClusterUpgradeProperties clusterUpgradeProperties = clusterUpgradePropertiesResolver.resolve(request);
        ServiceUpgradeValidationRequest validationRequest = new ServiceUpgradeValidationRequest(stackDtoService.getById(stackId), clusterUpgradeProperties);
        lockedComponentsValidator.validate(validationRequest);
        try {
            LOGGER.debug("Running the following upgrade validations: {}", serviceUpgradeValidators);
            serviceUpgradeValidators.forEach(validator -> validator.validate(validationRequest));
            return new ClusterUpgradeValidationFinishedEvent(stackId, clusterUpgradeProperties.getTargetImageId(), clusterUpgradeProperties);
        } catch (UpgradeValidationFailedException e) {
            LOGGER.warn("Cluster upgrade service validation failed", e);
            return new ClusterUpgradeValidationFailureEvent(stackId, e);
        } catch (Exception e) {
            LOGGER.error("Cluster upgrade service validation was unsuccessful due to an internal error", e);
            return new ClusterUpgradeValidationFinishedEvent(stackId, clusterUpgradeProperties.getTargetImageId(), clusterUpgradeProperties, e);
        }
    }

    @Override
    public String selector() {
        return VALIDATE_SERVICES_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeServiceValidationEvent> event) {
        LOGGER.warn("Cluster upgrade service validation failed", e);
        return new ClusterUpgradeValidationFailureEvent(resourceId, e);
    }
}
