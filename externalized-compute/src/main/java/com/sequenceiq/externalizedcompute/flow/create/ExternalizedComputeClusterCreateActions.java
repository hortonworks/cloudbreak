package com.sequenceiq.externalizedcompute.flow.create;

import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateEvent.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.create.ExternalizedComputeClusterCreateEvent.EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINALIZED_EVENT;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.externalizedcompute.entity.ExternalizedComputeClusterStatusEnum;
import com.sequenceiq.externalizedcompute.flow.AbstractExternalizedComputeClusterAction;
import com.sequenceiq.externalizedcompute.flow.AbstractExternalizedComputeClusterFailureAction;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterContext;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterEvent;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterCreateService;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterStatusService;

@Configuration
public class ExternalizedComputeClusterCreateActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterCreateActions.class);

    @Inject
    private ExternalizedComputeClusterCreateService externalizedComputeClusterCreateService;

    @Inject
    private ExternalizedComputeClusterStatusService externalizedComputeClusterStatusService;

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_CREATE_WAIT_ENV_STATE")
    public Action<?, ?> externalizedComputeClusterCreate() {
        return new AbstractExternalizedComputeClusterAction<>(ExternalizedComputeClusterEvent.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterEvent payload, Map<Object, Object> variables) {
                ExternalizedComputeClusterCreateEnvWaitRequest externalizedComputeClusterCreateEnvWaitRequest =
                        new ExternalizedComputeClusterCreateEnvWaitRequest(context.getExternalizedComputeId(), context.getActorCrn());
                sendEvent(context, externalizedComputeClusterCreateEnvWaitRequest.selector(), externalizedComputeClusterCreateEnvWaitRequest);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterEvent payload, Optional<ExternalizedComputeClusterContext> flowContext, Exception ex) {
                return ExternalizedComputeClusterCreateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_CREATE_IN_PROGRESS_STATE")
    public Action<?, ?> externalizedComputeClusterCreateWait() {
        return new AbstractExternalizedComputeClusterAction<>(ExternalizedComputeClusterCreateEnvWaitSuccessResponse.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterCreateEnvWaitSuccessResponse payload,
                    Map<Object, Object> variables) {
                externalizedComputeClusterCreateService.initiateCreation(context.getExternalizedComputeId(), context.getActorCrn());
                ExternalizedComputeClusterCreateWaitRequest externalizedComputeClusterCreateWaitRequest =
                        new ExternalizedComputeClusterCreateWaitRequest(context.getExternalizedComputeId(), context.getActorCrn());
                sendEvent(context, externalizedComputeClusterCreateWaitRequest.selector(), externalizedComputeClusterCreateWaitRequest);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterCreateEnvWaitSuccessResponse payload,
                    Optional<ExternalizedComputeClusterContext> flowContext, Exception ex) {
                return ExternalizedComputeClusterCreateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINISHED_STATE")
    public Action<?, ?> externalizedComputeClusterCreateFinished() {
        return new AbstractExternalizedComputeClusterAction<>(ExternalizedComputeClusterCreateWaitSuccessResponse.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterCreateWaitSuccessResponse payload,
                    Map<Object, Object> variables) {
                externalizedComputeClusterStatusService.setStatus(context.getExternalizedComputeId(), ExternalizedComputeClusterStatusEnum.AVAILABLE,
                        "Cluster provision finished");
                sendEvent(context, EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterCreateWaitSuccessResponse payload,
                    Optional<ExternalizedComputeClusterContext> flowContext, Exception ex) {
                return ExternalizedComputeClusterCreateFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAILED_STATE")
    public Action<?, ?> externalizedComputeClusterCreateFailed() {
        return new AbstractExternalizedComputeClusterFailureAction<>(ExternalizedComputeClusterCreateFailedEvent.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterCreateFailedEvent payload,
                    Map<Object, Object> variables) {
                String reason = "unknown reason";
                if (payload.getException() != null && payload.getException().getMessage() != null) {
                    reason = payload.getException().getMessage();
                }
                externalizedComputeClusterStatusService.setStatus(context.getExternalizedComputeId(), ExternalizedComputeClusterStatusEnum.CREATE_FAILED,
                        "Cluster provision failed due to: " + reason);
                LOGGER.warn("Cluster provision failed", payload.getException());
                sendEvent(context, EXTERNALIZED_COMPUTE_CLUSTER_CREATE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

}
