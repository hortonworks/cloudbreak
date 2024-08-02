package com.sequenceiq.externalizedcompute.flow.delete;

import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_AUX_CLUSTER_DELETE_STARTED;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINALIZED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteFlowEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STARTED_EVENT;

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
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterStatusService;

@Configuration
public class ExternalizedComputeClusterDeleteActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterDeleteActions.class);

    @Inject
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Inject
    private ExternalizedComputeClusterStatusService externalizedComputeClusterStatusService;

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_AUXILIARY_DELETE_STATE")
    public Action<?, ?> auxiliaryClusterDelete() {
        return new AbstractExternalizedComputeClusterAction<>(ExternalizedComputeClusterDeleteEvent.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterDeleteEvent payload, Map<Object, Object> variables) {
                externalizedComputeClusterStatusService.setStatus(context.getExternalizedComputeId(), ExternalizedComputeClusterStatusEnum.DELETE_IN_PROGRESS,
                        "Auxiliary cluster delete initiated");
                try {
                    externalizedComputeClusterService.initiateAuxClusterDelete(context.getExternalizedComputeId(), context.getActorCrn());
                    sendEvent(context, EXTERNALIZED_COMPUTE_CLUSTER_DELETE_AUX_CLUSTER_DELETE_STARTED.event(), payload);
                } catch (Exception e) {
                    if (payload.isForce()) {
                        LOGGER.warn("Auxiliary cluster delete failed: {}", e.getMessage(), e);
                        sendEvent(context, EXTERNALIZED_COMPUTE_CLUSTER_DELETE_AUX_CLUSTER_DELETE_STARTED.event(), payload);
                    } else {
                        ExternalizedComputeClusterDeleteFailedEvent failedEvent = ExternalizedComputeClusterDeleteFailedEvent.from(payload, e);
                        sendEvent(context, failedEvent.selector(), failedEvent);
                    }
                }
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterDeleteEvent payload, Optional<ExternalizedComputeClusterContext> flowContext,
                    Exception ex) {
                return ExternalizedComputeClusterDeleteFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_AUXILIARY_DELETE_IN_PROGRESS_STATE")
    public Action<?, ?> auxiliaryClusterDeleteWait() {
        return new AbstractExternalizedComputeClusterAction<>(ExternalizedComputeClusterDeleteEvent.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterDeleteEvent payload, Map<Object, Object> variables) {
                ExternalizedComputeClusterAuxiliaryDeleteWaitRequest auxiliaryDeleteWaitRequest =
                        new ExternalizedComputeClusterAuxiliaryDeleteWaitRequest(context.getExternalizedComputeId(), context.getActorCrn(), payload.isForce(),
                                payload.isPreserveCluster());
                sendEvent(context, auxiliaryDeleteWaitRequest.selector(), auxiliaryDeleteWaitRequest);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterDeleteEvent payload, Optional<ExternalizedComputeClusterContext> flowContext,
                    Exception ex) {
                return ExternalizedComputeClusterDeleteFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STATE")
    public Action<?, ?> externalizedComputeClusterDelete() {
        return new AbstractExternalizedComputeClusterAction<>(ExternalizedComputeClusterDeleteEvent.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterDeleteEvent payload, Map<Object, Object> variables) {
                ExternalizedComputeClusterStatusEnum deleteStatus = payload.isPreserveCluster() ?
                        ExternalizedComputeClusterStatusEnum.REINITIALIZE_IN_PROGRESS : ExternalizedComputeClusterStatusEnum.DELETE_IN_PROGRESS;
                externalizedComputeClusterStatusService.setStatus(context.getExternalizedComputeId(), deleteStatus,
                        "Cluster delete initiated");
                externalizedComputeClusterService.initiateDelete(context.getExternalizedComputeId());
                sendEvent(context, EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STARTED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterDeleteEvent payload, Optional<ExternalizedComputeClusterContext> flowContext,
                    Exception ex) {
                return ExternalizedComputeClusterDeleteFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_DELETE_IN_PROGRESS_STATE")
    public Action<?, ?> externalizedComputeClusterDeleteWait() {
        return new AbstractExternalizedComputeClusterAction<>(ExternalizedComputeClusterDeleteEvent.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterDeleteEvent payload, Map<Object, Object> variables) {
                ExternalizedComputeClusterDeleteWaitRequest externalizedComputeClusterDeleteWaitRequest =
                        new ExternalizedComputeClusterDeleteWaitRequest(context.getExternalizedComputeId(), context.getActorCrn(), payload.isForce(),
                                payload.isPreserveCluster());
                sendEvent(context, externalizedComputeClusterDeleteWaitRequest.selector(), externalizedComputeClusterDeleteWaitRequest);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterDeleteEvent payload, Optional<ExternalizedComputeClusterContext> flowContext,
                    Exception ex) {
                return ExternalizedComputeClusterDeleteFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINISHED_STATE")
    public Action<?, ?> externalizedComputeClusterDeleteFinished() {
        return new AbstractExternalizedComputeClusterAction<>(ExternalizedComputeClusterDeleteEvent.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterDeleteEvent payload, Map<Object, Object> variables) {
                if (payload.isPreserveCluster()) {
                    externalizedComputeClusterStatusService.setStatus(payload.getResourceId(), ExternalizedComputeClusterStatusEnum.REINITIALIZE_IN_PROGRESS,
                            "Cluster delete finished. Starting new cluster creation.");
                    externalizedComputeClusterService.deleteLiftieClusterNameForCluster(payload.getResourceId());
                } else {
                    externalizedComputeClusterService.deleteExternalizedComputeCluster(payload.getResourceId());
                }
                sendEvent(context, EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterDeleteEvent payload, Optional<ExternalizedComputeClusterContext> flowContext,
                    Exception ex) {
                return ExternalizedComputeClusterDeleteFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAILED_STATE")
    public Action<?, ?> externalizedComputeClusterDeleteFailed() {
        return new AbstractExternalizedComputeClusterFailureAction<>(ExternalizedComputeClusterDeleteFailedEvent.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterDeleteFailedEvent payload,
                    Map<Object, Object> variables) {
                String reason = "unknown reason";
                if (payload.getException() != null && payload.getException().getMessage() != null) {
                    reason = payload.getException().getMessage();
                }
                externalizedComputeClusterStatusService.setStatus(context.getExternalizedComputeId(), ExternalizedComputeClusterStatusEnum.DELETE_FAILED,
                        "Cluster deletion failed due to: " + reason);
                LOGGER.warn("Cluster deletion failed", payload.getException());
                sendEvent(context, EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

}
