package com.sequenceiq.redbeams.flow.redbeams.changeinstancetype;

import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.SYNC_DATABASE_SERVER_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.VALIDATE_CHANGE_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeHandlerSelectors.WAIT_FOR_CHANGE_INSTANCE_TYPE_HANDLER;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_ALREADY_APPLIED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.CHANGE_INSTANCE_TYPE_VALIDATION_FAILED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.FINALIZE_CHANGE_INSTANCE_TYPE_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors.HANDLED_FAILED_CHANGE_INSTANCE_TYPE_EVENT;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeEvent;
import com.sequenceiq.redbeams.flow.redbeams.changeinstancetype.event.ChangeInstanceTypeStateSelectors;
import com.sequenceiq.redbeams.flow.redbeams.common.AbstractRedbeamsFailureAction;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import com.sequenceiq.redbeams.service.stack.DBStackUpdater;

/**
 * Changes the instance type of a database server. The live instance type, storage size and HA state are read from the cloud provider first,
 * so the validation runs against the real values. If the provider already runs on the target instance type, the modification is skipped and
 * only the metadata is updated. Otherwise the modification is issued and polled until the provider applied it. The values persisted at the
 * end are always the live ones read from the provider, never the requested ones.
 */
@Configuration
public class ChangeInstanceTypeActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeInstanceTypeActions.class);

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private DBStackUpdater dbStackUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    @Bean(name = "CHANGE_INSTANCE_TYPE_SYNC_STATE")
    public Action<?, ?> syncInstanceTypeAction() {
        return new AbstractChangeInstanceTypeAction<>(ChangeInstanceTypeEvent.class) {

            @Override
            protected void doExecute(RedbeamsContext context, ChangeInstanceTypeEvent payload, Map<Object, Object> variables) {
                LOGGER.info("Changing the instance type of database server {} to '{}'", context.getDBStack().getResourceCrn(),
                        payload.getTargetInstanceType());
                dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.CHANGE_INSTANCE_TYPE_IN_PROGRESS);
                sendHandlerRequest(context, SYNC_DATABASE_SERVER_INSTANCE_TYPE_HANDLER, payload);
            }
        };
    }

    @Bean(name = "CHANGE_INSTANCE_TYPE_VALIDATION_STATE")
    public Action<?, ?> validateChangeInstanceTypeAction() {
        return new AbstractChangeInstanceTypeAction<>(ChangeInstanceTypeEvent.class) {

            @Override
            protected void doExecute(RedbeamsContext context, ChangeInstanceTypeEvent payload, Map<Object, Object> variables) {
                DatabaseServerInstanceTypeState liveState = payload.getLiveState();
                if (StringUtils.equalsIgnoreCase(payload.getTargetInstanceType(), liveState.instanceType()) && !liveState.modificationInProgress()) {
                    LOGGER.info("Database server {} already runs on instance type '{}', only the metadata is updated",
                            context.getDBStack().getResourceCrn(), liveState.instanceType());
                    sendEvent(context, new ChangeInstanceTypeEvent(CHANGE_INSTANCE_TYPE_ALREADY_APPLIED_EVENT.event(), payload.getResourceId(),
                            payload.getTargetInstanceType(), liveState));
                } else {
                    sendHandlerRequest(context, VALIDATE_CHANGE_INSTANCE_TYPE_HANDLER, payload);
                }
            }
        };
    }

    @Bean(name = "CHANGE_INSTANCE_TYPE_MODIFY_STATE")
    public Action<?, ?> changeInstanceTypeAction() {
        return new AbstractChangeInstanceTypeAction<>(ChangeInstanceTypeEvent.class) {

            @Override
            protected void doExecute(RedbeamsContext context, ChangeInstanceTypeEvent payload, Map<Object, Object> variables) {
                sendHandlerRequest(context, CHANGE_DATABASE_SERVER_INSTANCE_TYPE_HANDLER, payload);
            }
        };
    }

    @Bean(name = "CHANGE_INSTANCE_TYPE_WAIT_STATE")
    public Action<?, ?> waitForChangeInstanceTypeAction() {
        return new AbstractChangeInstanceTypeAction<>(ChangeInstanceTypeEvent.class) {

            @Override
            protected void doExecute(RedbeamsContext context, ChangeInstanceTypeEvent payload, Map<Object, Object> variables) {
                sendHandlerRequest(context, WAIT_FOR_CHANGE_INSTANCE_TYPE_HANDLER, payload);
            }
        };
    }

    @Bean(name = "CHANGE_INSTANCE_TYPE_FINISHED_STATE")
    public Action<?, ?> changeInstanceTypeFinishedAction() {
        return new AbstractChangeInstanceTypeAction<>(ChangeInstanceTypeEvent.class) {

            @Override
            protected void doExecute(RedbeamsContext context, ChangeInstanceTypeEvent payload, Map<Object, Object> variables) {
                DatabaseServerInstanceTypeState liveState = payload.getLiveState();
                dbStackUpdater.updateInstanceTypeAndStorageSize(payload.getResourceId(), liveState.instanceType(), liveState.storageSizeInGb());
                Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.CHANGE_INSTANCE_TYPE_COMPLETED);
                metricService.incrementMetricCounter(MetricType.DB_CHANGE_INSTANCE_TYPE_FINISHED, dbStack);
                LOGGER.info("Changing the instance type of database server {} finished, it runs on instance type '{}'",
                        context.getDBStack().getResourceCrn(), liveState.instanceType());
                sendEvent(context, new RedbeamsEvent(FINALIZE_CHANGE_INSTANCE_TYPE_EVENT.event(), payload.getResourceId()));
            }
        };
    }

    @Bean(name = "CHANGE_INSTANCE_TYPE_FAILED_STATE")
    public Action<?, ?> changeInstanceTypeFailedAction() {
        return new AbstractRedbeamsFailureAction<ChangeInstanceTypeState, ChangeInstanceTypeStateSelectors>() {

            @Override
            protected void prepareExecution(RedbeamsFailureEvent payload, Map<Object, Object> variables) {
                Exception failureException = payload.getException();
                LOGGER.error("Error during changing the instance type of the database server:", failureException);
                if (failureException instanceof CancellationException || ExceptionUtils.getRootCause(failureException) instanceof CancellationException) {
                    LOGGER.debug("The flow has been cancelled");
                } else {
                    String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
                    Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), getFailedStatus(payload), errorReason);
                    metricService.incrementMetricCounter(MetricType.DB_CHANGE_INSTANCE_TYPE_FAILED, dbStack);
                }
            }

            /**
             * A rejected validation leaves the database server unmodified, so it stays available. The dedicated validation failed event lets the
             * caller tell a rejected request apart from a failed modification, which needs a retry, without inspecting the exception type.
             */
            private DetailedDBStackStatus getFailedStatus(RedbeamsFailureEvent payload) {
                return CHANGE_INSTANCE_TYPE_VALIDATION_FAILED_EVENT.event().equals(payload.selector())
                        ? DetailedDBStackStatus.CHANGE_INSTANCE_TYPE_VALIDATION_FAILED
                        : DetailedDBStackStatus.CHANGE_INSTANCE_TYPE_FAILED;
            }

            @Override
            protected void doExecute(CommonContext context, RedbeamsFailureEvent payload, Map<Object, Object> variables) {
                sendEvent(context, new RedbeamsEvent(HANDLED_FAILED_CHANGE_INSTANCE_TYPE_EVENT.event(), payload.getResourceId()));
            }
        };
    }
}
