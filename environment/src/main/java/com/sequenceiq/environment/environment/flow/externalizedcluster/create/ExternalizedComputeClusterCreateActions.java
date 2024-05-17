package com.sequenceiq.environment.environment.flow.externalizedcluster.create;

import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationStateSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_HANDLED_EVENT;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationStateSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_FINALIZED_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationEvent;
import com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationFailedEvent;
import com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class ExternalizedComputeClusterCreateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterCreateActions.class);

    @Inject
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @Inject
    private ExternalizedComputeService externalizedComputeService;

    @Inject
    private EnvironmentService environmentService;

    @Bean(name = "DEFAULT_COMPUTE_CLUSTER_CREATION_START_STATE")
    public Action<?, ?> defaultComputeClusterCreation() {
        return new AbstractExternalizedComputeCreationAction<>(ExternalizedComputeClusterCreationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, ExternalizedComputeClusterCreationEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Default compute cluster creation started.");
                EnvironmentStatus environmentStatus = EnvironmentStatus.COMPUTE_CLUSTER_CREATION_IN_PROGRESS;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_CREATION_STARTED;
                EnvironmentDto envDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, environmentStatus, resourceEvent,
                        ExternalizedComputeClusterCreationState.DEFAULT_COMPUTE_CLUSTER_CREATION_START_STATE);
                Environment environment = environmentService.findEnvironmentByIdOrThrow(envDto.getId());
                externalizedComputeService.createComputeCluster(environment);
                sendEvent(context, ExternalizedComputeClusterCreationHandlerSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_WAIT_HANDLER_EVENT.selector(), envDto);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterCreationEvent payload, Optional<CommonContext> flowContext, Exception ex) {
                return new ExternalizedComputeClusterCreationFailedEvent(environmentService.internalGetByCrn(payload.getResourceCrn()), ex,
                        EnvironmentStatus.AVAILABLE);
            }
        };
    }

    @Bean(name = "DEFAULT_COMPUTE_CLUSTER_CREATION_FINISHED_STATE")
    public Action<?, ?> defaultComputeClusterCreationFinished() {
        return new AbstractExternalizedComputeCreationAction<>(ExternalizedComputeClusterCreationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, ExternalizedComputeClusterCreationEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Default compute cluster creation finished.");
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                                ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_CREATION_FINISHED,
                                ExternalizedComputeClusterCreationState.DEFAULT_COMPUTE_CLUSTER_CREATION_FINISHED_STATE);
                sendEvent(context, DEFAULT_COMPUTE_CLUSTER_CREATION_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterCreationEvent payload, Optional<CommonContext> flowContext, Exception ex) {
                return new ExternalizedComputeClusterCreationFailedEvent(environmentService.internalGetByCrn(payload.getResourceCrn()), ex,
                        EnvironmentStatus.AVAILABLE);
            }
        };
    }

    @Bean(name = "DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_STATE")
    public Action<?, ?> defaultComputeClusterCreationFailed() {
        return new AbstractExternalizedComputeCreationAction<>(ExternalizedComputeClusterCreationFailedEvent.class) {

            @Override
            protected void doExecute(CommonContext context, ExternalizedComputeClusterCreationFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.error("Default compute cluster creation failed.", payload.getException());
                environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                                ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_CREATION_FAILED, List.of(payload.getException().getMessage()),
                                ExternalizedComputeClusterCreationState.DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_STATE);
                sendEvent(context, DEFAULT_COMPUTE_CLUSTER_CREATION_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterCreationFailedEvent payload, Optional<CommonContext> flowContext, Exception ex) {
                return payload;
            }
        };
    }
}
