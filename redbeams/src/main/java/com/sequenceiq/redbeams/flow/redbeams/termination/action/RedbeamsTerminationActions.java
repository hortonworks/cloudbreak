package com.sequenceiq.redbeams.flow.redbeams.termination.action;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
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
import com.sequenceiq.redbeams.service.stack.DBStackService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;

@Configuration
public class RedbeamsTerminationActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsTerminationActions.class);

    @Inject
    private DBStackService dbStackService;

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Bean(name = "DEREGISTER_DATABASE_SERVER_STATE")
    public Action<?, ?> deregisterDatabaseServer() {
        return new AbstractRedbeamsTerminationAction<>(TerminateDatabaseServerSuccess.class) {

            @Override
            protected void prepareExecution(TerminateDatabaseServerSuccess payload, Map<Object, Object> variables) {
                dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.DEREGISTERING);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new DeregisterDatabaseServerRequest(context.getCloudContext(), context.getDatabaseStack(),
                        context.getDBStack());
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
        };
    }

    @Bean(name = "REDBEAMS_TERMINATION_FINISHED_STATE")
    public Action<?, ?> terminationFinished() {
        return new AbstractRedbeamsTerminationAction<>(DeregisterDatabaseServerSuccess.class) {

            @Override
            protected void prepareExecution(DeregisterDatabaseServerSuccess payload, Map<Object, Object> variables) {
                dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.DELETE_COMPLETED);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                // Delete the DB stack here instead of deregistration so that we can keep track of its status
                // through the termination
                dbStackService.delete(context.getDBStack());
                return new RedbeamsEvent(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_FINISHED_EVENT.name(), 0L);
            }
        };
    }

    @Bean(name = "REDBEAMS_TERMINATION_FAILED_STATE")
    public Action<?, ?> terminateFailed() {
        return new AbstractRedbeamsTerminationAction<>(RedbeamsFailureEvent.class) {

            // A lot here - some of this could go into some sort of failure handler class
            // compare to core StackCreationService::handleStackCreationFailure

            @Override
            protected void prepareExecution(RedbeamsFailureEvent payload, Map<Object, Object> variables) {

                Exception failureException = payload.getException();
                LOGGER.info("Error during database stack termination flow:", failureException);

                if (failureException instanceof CancellationException || ExceptionUtils.getRootCause(failureException) instanceof CancellationException) {
                    LOGGER.debug("The flow has been cancelled");
                } else {
                    // StackCreationActions / StackCreationService only update status if stack isn't mid-deletion
                    String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
                    dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.DELETE_FAILED, errorReason);
                }

            }

            @Override
            protected RedbeamsContext createFlowContext(FlowParameters flowParameters,
                                                        StateContext<RedbeamsTerminationState, RedbeamsTerminationEvent> stateContext,
                                                        RedbeamsFailureEvent payload) {

                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());

                // FIXME perhaps better to use something like StackFailureContext / AbstractStackFailureAction
                // - leave out cloud context, cloud credentials, converted database stack
                // - only "view" of dbstack, but not sure why that matters though

                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new RedbeamsEvent(RedbeamsTerminationEvent.REDBEAMS_TERMINATION_FAILURE_HANDLED_EVENT.event(), 0L);
            }
        };
    }
}
