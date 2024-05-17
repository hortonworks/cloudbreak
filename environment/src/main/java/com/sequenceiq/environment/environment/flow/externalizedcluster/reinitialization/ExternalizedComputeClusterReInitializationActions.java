package com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization;

import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_HANDLED_EVENT;
import static com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINALIZED_EVENT;

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
import com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationEvent;
import com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationFailedEvent;
import com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationHandlerSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.EnvironmentStatusUpdateService;
import com.sequenceiq.environment.environment.service.externalizedcompute.ExternalizedComputeService;
import com.sequenceiq.flow.core.CommonContext;

@Configuration
public class ExternalizedComputeClusterReInitializationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterReInitializationActions.class);

    @Inject
    private EnvironmentStatusUpdateService environmentStatusUpdateService;

    @Inject
    private ExternalizedComputeService externalizedComputeService;

    @Inject
    private EnvironmentService environmentService;

    @Bean(name = "DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_STATE")
    public Action<?, ?> defaultComputeClusterReinitialization() {
        return new AbstractExternalizedComputeReInitializationAction<>(ExternalizedComputeClusterReInitializationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, ExternalizedComputeClusterReInitializationEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Re-initializing external cluster started.");
                EnvironmentStatus environmentStatus = EnvironmentStatus.COMPUTE_CLUSTER_REINITIALIZATION_IN_PROGRESS;
                ResourceEvent resourceEvent = ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_REINITIALIZATION_STARTED;
                EnvironmentDto envDto = environmentStatusUpdateService.updateEnvironmentStatusAndNotify(context, payload, environmentStatus, resourceEvent,
                        ExternalizedComputeClusterReInitializationState.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_STATE);
                Environment environment = environmentService.findEnvironmentByIdOrThrow(envDto.getId());
                externalizedComputeService.reInitializeComputeCluster(environment, payload.isForce());
                sendEvent(context,
                        ExternalizedComputeClusterReInitializationHandlerSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_WAIT_HANDLER_EVENT.selector(),
                        envDto);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterReInitializationEvent payload, Optional<CommonContext> flowContext, Exception ex) {
                return new ExternalizedComputeClusterReInitializationFailedEvent(environmentService.internalGetByCrn(payload.getResourceCrn()), ex,
                        EnvironmentStatus.AVAILABLE);
            }
        };
    }

    @Bean(name = "DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_STATE")
    public Action<?, ?> defaultComputeClusterReinitializationFinished() {
        return new AbstractExternalizedComputeReInitializationAction<>(ExternalizedComputeClusterReInitializationEvent.class) {

            @Override
            protected void doExecute(CommonContext context, ExternalizedComputeClusterReInitializationEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Re-initializing external cluster finished.");
                environmentStatusUpdateService
                        .updateEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                                ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED,
                                ExternalizedComputeClusterReInitializationState.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINISHED_STATE);
                sendEvent(context, DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterReInitializationEvent payload, Optional<CommonContext> flowContext, Exception ex) {
                return new ExternalizedComputeClusterReInitializationFailedEvent(environmentService.internalGetByCrn(payload.getResourceCrn()), ex,
                        EnvironmentStatus.AVAILABLE);
            }
        };
    }

    @Bean(name = "DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_STATE")
    public Action<?, ?> defaultComputeClusterReinitializationFailed() {
        return new AbstractExternalizedComputeReInitializationAction<>(ExternalizedComputeClusterReInitializationFailedEvent.class) {

            @Override
            protected void doExecute(CommonContext context, ExternalizedComputeClusterReInitializationFailedEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Re-initializing external cluster failed.", payload.getException());
                environmentStatusUpdateService
                        .updateFailedEnvironmentStatusAndNotify(context, payload, EnvironmentStatus.AVAILABLE,
                                ResourceEvent.ENVIRONMENT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED, List.of(payload.getException().getMessage()),
                                ExternalizedComputeClusterReInitializationState.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_STATE);
                sendEvent(context, DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_FAILED_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterReInitializationFailedEvent payload, Optional<CommonContext> flowContext,
                    Exception ex) {
                return payload;
            }
        };
    }
}
