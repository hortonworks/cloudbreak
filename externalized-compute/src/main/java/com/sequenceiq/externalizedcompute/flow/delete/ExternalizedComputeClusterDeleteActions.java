package com.sequenceiq.externalizedcompute.flow.delete;

import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAIL_HANDLED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINALIZED_EVENT;
import static com.sequenceiq.externalizedcompute.flow.delete.ExternalizedComputeClusterDeleteEvent.EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STARTED_EVENT;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.externalizedcompute.flow.AbstractExternalizedComputeClusterAction;
import com.sequenceiq.externalizedcompute.flow.AbstractExternalizedComputeClusterFailureAction;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterContext;
import com.sequenceiq.externalizedcompute.flow.ExternalizedComputeClusterEvent;
import com.sequenceiq.externalizedcompute.service.ExternalizedComputeClusterService;

@Configuration
public class ExternalizedComputeClusterDeleteActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterDeleteActions.class);

    @Inject
    private ExternalizedComputeClusterService externalizedComputeClusterService;

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STATE")
    public Action<?, ?> externalizedComputeClusterDelete() {
        return new AbstractExternalizedComputeClusterAction<>(ExternalizedComputeClusterEvent.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterEvent payload, Map<Object, Object> variables) {
                externalizedComputeClusterService.initiateDelete(context.getExternalizedComputeId());
                sendEvent(context, EXTERNALIZED_COMPUTE_CLUSTER_DELETE_STARTED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterEvent payload, Optional<ExternalizedComputeClusterContext> flowContext, Exception ex) {
                return ExternalizedComputeClusterDeleteFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_DELETE_IN_PROGRESS_STATE")
    public Action<?, ?> externalizedComputeClusterDeleteWait() {
        return new AbstractExternalizedComputeClusterAction<>(ExternalizedComputeClusterEvent.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterEvent payload, Map<Object, Object> variables) {
                ExternalizedComputeClusterDeleteWaitRequest externalizedComputeClusterDeleteWaitRequest =
                        new ExternalizedComputeClusterDeleteWaitRequest(context.getExternalizedComputeId(), context.getUserId());
                sendEvent(context, externalizedComputeClusterDeleteWaitRequest.selector(), externalizedComputeClusterDeleteWaitRequest);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterEvent payload, Optional<ExternalizedComputeClusterContext> flowContext, Exception ex) {
                return ExternalizedComputeClusterDeleteFailedEvent.from(payload, ex);
            }
        };
    }

    @Bean(name = "EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINISHED_STATE")
    public Action<?, ?> externalizedComputeClusterDeleteFinished() {
        return new AbstractExternalizedComputeClusterAction<>(ExternalizedComputeClusterEvent.class) {

            @Override
            protected void doExecute(ExternalizedComputeClusterContext context, ExternalizedComputeClusterEvent payload, Map<Object, Object> variables) {
                externalizedComputeClusterService.deleteExternalizedComputeCluster(payload.getResourceId());
                sendEvent(context, EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FINALIZED_EVENT.event(), payload);
            }

            @Override
            protected Object getFailurePayload(ExternalizedComputeClusterEvent payload, Optional<ExternalizedComputeClusterContext> flowContext, Exception ex) {
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
                LOGGER.warn("Externalized compute cluster delete failed", payload.getException());
                sendEvent(context, EXTERNALIZED_COMPUTE_CLUSTER_DELETE_FAIL_HANDLED_EVENT.event(), payload);
            }
        };
    }

}
