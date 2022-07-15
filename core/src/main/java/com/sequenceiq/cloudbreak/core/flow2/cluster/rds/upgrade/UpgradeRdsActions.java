package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.domain.view.ClusterView;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataBackupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataBackupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataRestoreRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataRestoreResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStopServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStopServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerResult;

@Configuration
public class UpgradeRdsActions {

    @Inject
    private UpgradeRdsService upgradeRdsService;

    @Bean(name = "UPGRADE_RDS_STOP_SERVICES_STATE")
    public Action<?, ?> stopServicesAndCm() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsTriggerRequest.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsTriggerRequest payload, Map<Object, Object> variables) {
                upgradeRdsService.stopServicesState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeRdsContext context) {
                return new UpgradeRdsStopServicesRequest(context.getStackId(), context.getVersion());
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_DATA_BACKUP_STATE")
    public Action<?, ?> backupDataFromRds() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsStopServicesResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsStopServicesResult payload, Map<Object, Object> variables) {
                upgradeRdsService.backupRdsState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeRdsContext context) {
                return new UpgradeRdsDataBackupRequest(context.getStackId(), context.getVersion());
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_UPGRADE_DATABASE_SERVER_STATE")
    public Action<?, ?> upgradeDatabaseServer() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsDataBackupResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsDataBackupResult payload, Map<Object, Object> variables) {
                upgradeRdsService.upgradeRdsState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeRdsContext context) {
                return new UpgradeRdsUpgradeDatabaseServerRequest(context.getStackId(), context.getVersion());
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_DATA_RESTORE_STATE")
    public Action<?, ?> restoreDataToRds() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsUpgradeDatabaseServerResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsUpgradeDatabaseServerResult payload, Map<Object, Object> variables) {
                upgradeRdsService.restoreRdsState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeRdsContext context) {
                return new UpgradeRdsDataRestoreRequest(context.getStackId(), context.getVersion());
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_START_SERVICES_STATE")
    public Action<?, ?> restartServicesAndCm() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsDataRestoreResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsDataRestoreResult payload, Map<Object, Object> variables) {
                upgradeRdsService.startServicesState(payload.getResourceId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeRdsContext context) {
                return new UpgradeRdsStartServicesRequest(context.getStackId(), context.getVersion());
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_FINISHED_STATE")
    public Action<?, ?> upgradeRdsFinished() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsStartServicesResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsStartServicesResult payload, Map<Object, Object> variables) {
                upgradeRdsService.rdsUpgradeFinished(payload.getResourceId(), context.getClusterId());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeRdsContext context) {
                return new StackEvent(UpgradeRdsEvent.FINALIZED_EVENT.event(), context.getStackId());
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_FAILED_STATE")
    public Action<?, ?> upgradeRdsFailed() {
        return new AbstractStackFailureAction<UpgradeRdsState, UpgradeRdsEvent>() {
            @Override
            protected void doExecute(StackFailureContext context, StackFailureEvent payload, Map<Object, Object> variables) {
                UpgradeRdsFailedEvent concretePayload = (UpgradeRdsFailedEvent) payload;
                upgradeRdsService.rdsUpgradeFailed(concretePayload.getResourceId(),
                        Optional.ofNullable(context.getStackView().getClusterView()).map(ClusterView::getId).orElse(null),
                        concretePayload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(UpgradeRdsEvent.FAIL_HANDLED_EVENT.event(), context.getStackView().getId());
            }
        };
    }
}
