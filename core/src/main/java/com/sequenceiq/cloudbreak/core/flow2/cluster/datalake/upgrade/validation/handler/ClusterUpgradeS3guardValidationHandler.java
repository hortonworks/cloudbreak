package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeValidationHandlerSelectors.VALIDATE_S3GUARD_EVENT;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeS3guardValidationEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.validation.event.ClusterUpgradeS3guardValidationFinishedEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesResolver;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpgradeS3guardValidationHandler extends ExceptionCatcherEventHandler<ClusterUpgradeS3guardValidationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpgradeS3guardValidationHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private ClusterUpgradePropertiesResolver clusterUpgradePropertiesResolver;

    @Override
    public String selector() {
        return VALIDATE_S3GUARD_EVENT.selector();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpgradeS3guardValidationEvent> event) {
        LOGGER.error("Cluster upgrade S3guard validation was unsuccessful due to an unexpected error", e);
        ClusterUpgradeS3guardValidationEvent request = event.getData();
        ClusterUpgradeProperties clusterUpgradeProperties = clusterUpgradePropertiesResolver.resolveUnchecked(request);
        return new ClusterUpgradeS3guardValidationFinishedEvent(resourceId, clusterUpgradeProperties.getTargetImageId(), clusterUpgradeProperties);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpgradeS3guardValidationEvent> event) {
        LOGGER.debug("Accepting Cluster upgrade S3guard disabled validation event.");
        ClusterUpgradeS3guardValidationEvent request = event.getData();
        Long stackId = request.getResourceId();
        ClusterUpgradeProperties clusterUpgradeProperties = clusterUpgradePropertiesResolver.resolveUnchecked(request);
        try {
            String environmentCrn = stackService.findEnvironmentCrnByStackId(stackId);
            DetailedEnvironmentResponse environmentResponse = environmentService.getByCrn(environmentCrn);
            return new ClusterUpgradeS3guardValidationFinishedEvent(request.getResourceId(), clusterUpgradeProperties.getTargetImageId(),
                    clusterUpgradeProperties);
        } catch (Exception ex) {
            LOGGER.error("Cluster upgrade validation was unsuccessful due to an internal error", ex);
            return new ClusterUpgradeS3guardValidationFinishedEvent(request.getResourceId(), clusterUpgradeProperties.getTargetImageId(),
                    clusterUpgradeProperties);
        }
    }
}
