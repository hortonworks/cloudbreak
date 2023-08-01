package com.sequenceiq.redbeams.flow.redbeams.upgrade.action;

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
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.core.Flow;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.AbstractRedbeamsUpgradeAction;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.RedbeamsUpgradeState;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsStartUpgradeRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.RedbeamsUpgradeFailedEvent;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.UpgradeDatabaseServerRequest;
import com.sequenceiq.redbeams.flow.redbeams.upgrade.event.UpgradeDatabaseServerSuccess;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@Configuration
public class RedbeamsUpgradeActions {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedbeamsUpgradeActions.class);

    @Inject
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Inject
    private RedbeamsMetricService metricService;

    @Bean(name = "UPGRADE_DATABASE_SERVER_STATE")
    public Action<?, ?> upgradeDatabaseServer() {
        return new AbstractRedbeamsUpgradeAction<>(RedbeamsStartUpgradeRequest.class) {

            @Override
            protected void doExecute(RedbeamsContext context, RedbeamsStartUpgradeRequest payload, Map<Object, Object> variables) {
                TargetMajorVersion targetMajorVersion = payload.getTargetMajorVersion();
                sendEvent(context,
                        new UpgradeDatabaseServerRequest(
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

    @Bean(name = "REDBEAMS_UPGRADE_FINISHED_STATE")
    public Action<?, ?> upgradeFinished() {
        return new AbstractRedbeamsUpgradeAction<>(UpgradeDatabaseServerSuccess.class) {

            @Override
            protected void prepareExecution(UpgradeDatabaseServerSuccess payload, Map<Object, Object> variables) {
                Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.AVAILABLE);
                metricService.incrementMetricCounter(MetricType.DB_UPGRADE_FINISHED, dbStack);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new RedbeamsEvent(RedbeamsUpgradeEvent.REDBEAMS_UPGRADE_FINISHED_EVENT.name(), context.getDBStack().getId());
            }
        };
    }

    @Bean(name = "REDBEAMS_UPGRADE_FAILED_STATE")
    public Action<?, ?> upgradeFailed() {
        return new AbstractRedbeamsUpgradeAction<>(RedbeamsUpgradeFailedEvent.class) {

            @Override
            protected void prepareExecution(RedbeamsUpgradeFailedEvent payload, Map<Object, Object> variables) {

                Exception failureException = payload.getException();
                LOGGER.info("Error during database server upgrade flow:", failureException);

                if (failureException instanceof CancellationException || ExceptionUtils.getRootCause(failureException) instanceof CancellationException) {
                    LOGGER.debug("The flow has been cancelled");
                } else {
                    String errorReason = failureException == null ? "Unknown error" : failureException.getMessage();
                    Optional<DBStack> dbStack = dbStackStatusUpdater.updateStatus(payload.getResourceId(), DetailedDBStackStatus.UPGRADE_FAILED, errorReason);
                    metricService.incrementMetricCounter(MetricType.DB_UPGRADE_FAILED, dbStack);
                }

            }

            @Override
            protected RedbeamsContext createFlowContext(FlowParameters flowParameters,
                    StateContext<RedbeamsUpgradeState, RedbeamsUpgradeEvent> stateContext, RedbeamsUpgradeFailedEvent payload) {

                Flow flow = getFlow(flowParameters.getFlowId());
                flow.setFlowFailed(payload.getException());

                return super.createFlowContext(flowParameters, stateContext, payload);
            }

            @Override
            protected Selectable createRequest(RedbeamsContext context) {
                return new RedbeamsEvent(RedbeamsUpgradeEvent.REDBEAMS_UPGRADE_FAILURE_HANDLED_EVENT.event(), context.getDBStack().getId());
            }
        };
    }

}