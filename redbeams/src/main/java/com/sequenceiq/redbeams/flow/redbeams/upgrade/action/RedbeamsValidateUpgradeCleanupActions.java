package com.sequenceiq.redbeams.flow.redbeams.upgrade.action;

import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupEvent.REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILURE_HANDLED_EVENT;
import static com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupEvent.REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FINISHED_EVENT;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.AbstractRedbeamsFailureAction;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.AbstractRedbeamsUpgradeAction;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeCleanupState;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartValidateUpgradeCleanupRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerCleanupRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerCleanupSuccess;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Configuration
public class RedbeamsValidateUpgradeCleanupActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsValidateUpgradeCleanupActions.class);

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    @Bean(name = "VALIDATE_UPGRADE_DATABASE_SERVER_CLEANUP_STATE")
    public Action<?, ?> validateUpgradeDatabaseServerCleanup() {
        return new AbstractRedbeamsUpgradeAction<>(RedbeamsStartValidateUpgradeCleanupRequest.class) {

            @Override
            protected void doExecute(RedbeamsContext context, RedbeamsStartValidateUpgradeCleanupRequest payload, Map<Object, Object> variables) {
                sendEvent(context,
                        new ValidateUpgradeDatabaseServerCleanupRequest(
                                context.getCloudContext(),
                                context.getCloudCredential(),
                                context.getDatabaseStack()
                        )
                );
            }
        };
    }

    @Bean(name = "REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FINISHED_STATE")
    public Action<?, ?> validateUpgradeFinished() {
        return new AbstractRedbeamsUpgradeAction<>(ValidateUpgradeDatabaseServerCleanupSuccess.class) {

            @Override
            protected void prepareExecution(ValidateUpgradeDatabaseServerCleanupSuccess payload, Map<Object, Object> variables) {
                Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.AVAILABLE);
                metricService.incrementMetricCounter(MetricType.DB_VALIDATE_UPGRADE_CLEANUP_FINISHED, dbStack);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new RedbeamsEvent(REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FINISHED_EVENT.name(), context.getDBStack().getId());
            }
        };
    }

    @Bean(name = "REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILED_STATE")
    public Action<?, ?> validateUpgradeFailed() {
        return new AbstractRedbeamsFailureAction<RedbeamsValidateUpgradeCleanupState, RedbeamsValidateUpgradeCleanupEvent>() {

            @Override
            protected void prepareExecution(RedbeamsFailureEvent payload, Map<Object, Object> variables) {

                Exception failureException = payload.getException();
                LOGGER.error("Error during upgrade database server validation cleanup flow:", failureException);

                if (failureException instanceof CancellationException || ExceptionUtils.getRootCause(failureException) instanceof CancellationException) {
                    LOGGER.debug("The flow has been cancelled");
                } else {
                    String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
                    Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(
                            payload.getResourceId(), DetailedDBStackStatus.VALIDATE_UPGRADE_CLEANUP_FAILED, errorReason);
                    metricService.incrementMetricCounter(MetricType.DB_VALIDATE_UPGRADE_CLEANUP_FAILED, dbStack);
                }

            }

            @Override
            protected void doExecute(CommonContext context, RedbeamsFailureEvent payload, Map<Object, Object> variables) {
                sendEvent(context, new RedbeamsEvent(REDBEAMS_VALIDATE_UPGRADE_CLEANUP_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
            }
        };
    }
}