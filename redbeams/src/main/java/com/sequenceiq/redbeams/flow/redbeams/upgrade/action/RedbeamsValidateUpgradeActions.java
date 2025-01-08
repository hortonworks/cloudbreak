package com.sequenceiq.redbeams.flow.redbeams.upgrade.action;

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

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.CommonContext;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.AbstractRedbeamsFailureAction;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsFailureEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.AbstractRedbeamsUpgradeAction;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsValidateUpgradeState;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartValidateUpgradeRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.ValidateUpgradeDatabaseServerSuccess;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Configuration
public class RedbeamsValidateUpgradeActions {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsValidateUpgradeActions.class);

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    @Bean(name = "VALIDATE_UPGRADE_DATABASE_SERVER_STATE")
    public Action<?, ?> validateUpgradeDatabaseServer() {
        return new AbstractRedbeamsUpgradeAction<>(RedbeamsStartValidateUpgradeRequest.class) {

            @Override
            protected void doExecute(RedbeamsContext context, RedbeamsStartValidateUpgradeRequest payload, Map<Object, Object> variables) {
                TargetMajorVersion targetMajorVersion = payload.getTargetMajorVersion();
                sendEvent(context,
                        new ValidateUpgradeDatabaseServerRequest(
                                context.getCloudContext(),
                                context.getCloudCredential(),
                                context.getDatabaseStack(),
                                targetMajorVersion,
                                payload.getMigrationParams()
                        )
                );
            }
        };
    }

    @Bean(name = "REDBEAMS_VALIDATE_UPGRADE_FINISHED_STATE")
    public Action<?, ?> validateUpgradeFinished() {
        return new AbstractRedbeamsUpgradeAction<>(ValidateUpgradeDatabaseServerSuccess.class) {

            @Override
            protected void prepareExecution(ValidateUpgradeDatabaseServerSuccess payload, Map<Object, Object> variables) {
                String validationWarningMessage = payload.getValidationWarningMessage();
                Optional<DBStack> dbStack = StringUtils.isNotEmpty(validationWarningMessage) ?
                        dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.AVAILABLE, validationWarningMessage) :
                        dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.AVAILABLE);
                metricService.incrementMetricCounter(MetricType.DB_VALIDATE_UPGRADE_FINISHED, dbStack);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new RedbeamsEvent(RedbeamsValidateUpgradeEvent.REDBEAMS_VALIDATE_UPGRADE_FINISHED_EVENT.name(), context.getDBStack().getId());
            }
        };
    }

    @Bean(name = "REDBEAMS_VALIDATE_UPGRADE_FAILED_STATE")
    public Action<?, ?> validateUpgradeFailed() {
        return new AbstractRedbeamsFailureAction<RedbeamsValidateUpgradeState, RedbeamsValidateUpgradeEvent>() {

            @Override
            protected void prepareExecution(RedbeamsFailureEvent payload, Map<Object, Object> variables) {

                Exception failureException = payload.getException();
                LOGGER.error("Error during database server validate upgrade flow:", failureException);

                if (failureException instanceof CancellationException || ExceptionUtils.getRootCause(failureException) instanceof CancellationException) {
                    LOGGER.debug("The flow has been cancelled");
                } else {
                    String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
                    Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(
                            payload.getResourceId(), DetailedDBStackStatus.VALIDATE_UPGRADE_FAILED, errorReason);
                    metricService.incrementMetricCounter(MetricType.DB_VALIDATE_UPGRADE_FAILED, dbStack);
                }
            }

            @Override
            protected void doExecute(CommonContext context, RedbeamsFailureEvent payload, Map<Object, Object> variables) {
                sendEvent(context, new RedbeamsEvent(
                        RedbeamsValidateUpgradeEvent.REDBEAMS_VALIDATE_UPGRADE_FAILURE_HANDLED_EVENT.event(), payload.getResourceId()));
            }
        };
    }
}