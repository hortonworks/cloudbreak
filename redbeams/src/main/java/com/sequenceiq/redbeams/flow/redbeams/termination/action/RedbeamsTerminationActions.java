package com.sequenceiq.redbeams.flow.redbeams.termination.action;

import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent.REDBEAMS_TERMINATION_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent.REDBEAMS_TERMINATION_FINISHED_EVENT;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.AbstractRedbeamsTerminationAction;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationEvent;
import com.sequenceiq.redbeams.flow.redbeams.termination.RedbeamsTerminationState;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.deregister.DeregisterDatabaseServerSuccess;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.termination.event.terminate.TerminateDatabaseServerSuccess;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import com.sequenceiq.redbeams.sync.DBStackJobService;

@Configuration
public class RedbeamsTerminationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsTerminationActions.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    @Inject
    private DBStackJobService dbStackJobService;

    @Bean(name = "DEREGISTER_DATABASE_SERVER_STATE")
    public Action<?, ?> deregisterDatabaseServer() {
        return new AbstractRedbeamsTerminationAction<>(TerminateDatabaseServerSuccess.class) {

            @Override
            protected void prepareExecution(TerminateDatabaseServerSuccess payload, Map<Object, Object> variables) {
                dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.DEREGISTERING);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                dbStackJobService.unschedule(context.getDBStack());
                return new DeregisterDatabaseServerRequest(context.getCloudContext(), context.getDatabaseStack(), context.getDBStack());
            }

            @Override
            protected Object getFailurePayload(TerminateDatabaseServerSuccess payload, Optional<RedbeamsContext> flowContext, Exception ex) {
                return new RedbeamsFailureEvent(payload.getResourceId(), ex, payload.isForced());
            }
        };
    }

    @Bean(name = "TERMINATE_DATABASE_SERVER_STATE")
    public Action<?, ?> terminateDatabaseServer() {
        return new AbstractRedbeamsTerminationAction<>(RedbeamsEvent.class) {

            private boolean force;

            @Override
            protected void prepareExecution(RedbeamsEvent payload, Map<Object, Object> variables) {
                dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.DELETE_IN_PROGRESS);
                force = payload.isForced();
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new TerminateDatabaseServerRequest(context.getCloudContext(), context.getCloudCredential(),
                        context.getDatabaseStack(), force);
            }

            @Override
            protected Object getFailurePayload(RedbeamsEvent payload, Optional<RedbeamsContext> flowContext, Exception ex) {
                return new RedbeamsFailureEvent(payload.getResourceId(), ex, payload.isForced());
            }
        };
    }

    @Bean(name = "REDBEAMS_TERMINATION_FINISHED_STATE")
    public Action<?, ?> terminationFinished() {
        return new AbstractRedbeamsTerminationAction<>(DeregisterDatabaseServerSuccess.class, false) {

            @Override
            protected void prepareExecution(DeregisterDatabaseServerSuccess payload, Map<Object, Object> variables) {
                dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.DELETE_COMPLETED);
            }

            @Override
            protected void doExecute(RedbeamsContext context, DeregisterDatabaseServerSuccess payload, Map<Object, Object> variables) throws Exception {
                // Delete the DB stack here instead of deregistration so that we can keep track of its status
                // through the termination
                Optional<DBStack> dbstack = Optional.ofNullable(context.getDBStack());
                metricService.incrementMetricCounter(MetricType.DB_TERMINATION_FINISHED, dbstack);
                dbstack.ifPresentOrElse(db -> dbStackService.delete(db.getId()),
                        () -> LOGGER.debug("DBStack for {} id is not found, so deletion will be skipped!", payload.getResourceId()));
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new RedbeamsEvent(REDBEAMS_TERMINATION_FINISHED_EVENT.name(), 0L);
            }
        };
    }

    @Bean(name = "REDBEAMS_TERMINATION_FAILED_STATE")
    public Action<?, ?> failedAction() {
        return new AbstractRedbeamsTerminationAction<>(RedbeamsFailureEvent.class, false) {

            // A lot here - some of this could go into some sort of failure handler class
            // compare to core StackCreationService::handleStackCreationFailure

            @Override
            protected void doExecute(RedbeamsContext context, RedbeamsFailureEvent payload, Map<Object, Object> variables) {
                Exception failureException = payload.getException();
                LOGGER.info("Error during database stack termination flow:", failureException);
                if (failureException instanceof CancellationException || ExceptionUtils.getRootCause(failureException) instanceof CancellationException) {
                    LOGGER.debug("The flow has been cancelled");
                } else {
                    // StackCreationActions / StackCreationService only update status if stack isn't mid-deletion
                    String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
                    Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.DELETE_FAILED, errorReason);
                    metricService.incrementMetricCounter(MetricType.DB_TERMINATION_FAILED, dbStack);
                }
                sendEvent(context, REDBEAMS_TERMINATION_FAILURE_HANDLED_EVENT.event(), payload);
            }

            @Override
            protected RedbeamsContext createFlowContext(FlowParameters flowParameters,
                StateContext<RedbeamsTerminationState,
                RedbeamsTerminationEvent> stateContext,
                RedbeamsFailureEvent payload) {

                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());

                // FIXME perhaps better to use something like StackFailureContext / AbstractStackFailureAction
                // - leave out cloud context, cloud credentials, converted database stack
                // - only "view" of dbstack, but not sure why that matters though

                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected Object getFailurePayload(RedbeamsFailureEvent payload, Optional<RedbeamsContext> flowContext, Exception ex) {
                return payload;
            }
        };
    }
}
