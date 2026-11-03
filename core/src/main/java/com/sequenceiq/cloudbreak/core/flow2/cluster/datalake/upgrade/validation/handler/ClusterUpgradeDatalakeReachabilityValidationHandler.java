package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_DATALAKE_REACHABILITY_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDatalakeReachabilityValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeDatalakeReachabilityValidationFinishedEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationFailureEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.datalake.DataLakeStatusCheckerService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeNotReachableException;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeDatalakeReachabilityValidationHandler extends ExceptionCatcherEventHandler<ClusterUpgradeDatalakeReachabilityValidationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeDatalakeReachabilityValidationHandler.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private DataLakeStatusCheckerService dataLakeStatusCheckerService;

    @Inject
    private ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver;

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeDatalakeReachabilityValidationEvent> event) {
        LOGGER.debug("Accepting Cluster upgrade Data Lake reachability validation event.");
        ClusterUpgradeDatalakeReachabilityValidationEvent request = event.getData();
        Long stackId = request.getResourceId();
        ClusterUpgradeProperties clusterUpgradeProperties = clusterUpgradePropertiesResolver.resolve(request);
        try {
            dataLakeStatusCheckerService.validateDatalakeReachable(stackDtoService.getStackViewById(stackId));
        } catch (DatalakeNotReachableException e) {
            LOGGER.info("Data Lake reachability validation failed with: {}", e.getMessage());
            return new ClusterUpgradeValidationFailureEvent(stackId, new UpgradeValidationFailedException(e.getMessage()));
        }
        LOGGER.debug("Data Lake reachability validation passed successfully");
        return new ClusterUpgradeDatalakeReachabilityValidationFinishedEvent(stackId, clusterUpgradeProperties.getTargetImageId(),
                clusterUpgradeProperties);
    }

    @Override
    public String selector() {
        return VALIDATE_DATALAKE_REACHABILITY_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeDatalakeReachabilityValidationEvent> event) {
        LOGGER.error("Cluster upgrade Data Lake reachability validation was unsuccessful due to an unexpected error", e);
        return new ClusterUpgradeValidationFailureEvent(resourceId, e);
    }
}
