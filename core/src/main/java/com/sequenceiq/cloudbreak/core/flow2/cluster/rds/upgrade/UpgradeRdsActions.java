package com.sequenceiq.cloudbreak.core.flow2.cluster.rds.upgrade;

import static com.sequenceiq.cloudbreak.util.NullUtil.putIfPresent;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.action.Action;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.core.flow2.stack.AbstractStackFailureAction;
import com.sequenceiq.cloudbreak.core.flow2.stack.StackFailureContext;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataBackupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataBackupResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataRestoreRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsDataRestoreResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsInstallPostgresPackagesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsInstallPostgresPackagesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateAttachedDatahubsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateAttachedDatahubsResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateDatabaseSettingsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateDatabaseSettingsResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateServicesDBSettingsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsMigrateServicesDBSettingsResponse;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartCMResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartCMServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStartCMServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStopServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsStopServicesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsTriggerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpdateVersionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpdateVersionResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.UpgradeRdsUpgradeDatabaseServerResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.WaitForDatabaseServerUpgradeRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.WaitForDatabaseServerUpgradeResult;
import com.sequenceiq.cloudbreak.view.StackView;

@Configuration
public class UpgradeRdsActions {

    private static final Object CLOUD_STORAGE_BACKUP_LOCATION = "cloud_storage_backup_location";

    private static final Object CLOUD_STORAGE_INSTANCE_PROFILE = "cloud_storage_instance_profile";

    @Inject
    private UpgradeRdsService upgradeRdsService;

    @Inject
    private ExternalDatabaseService externalDatabaseService;

    @Bean(name = "UPGRADE_RDS_STOP_SERVICES_STATE")
    public Action<?, ?> stopServicesAndCm() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsTriggerRequest.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsTriggerRequest payload, Map<Object, Object> variables) {
                putIfPresent(variables, CLOUD_STORAGE_BACKUP_LOCATION, payload.getBackupLocation());
                putIfPresent(variables, CLOUD_STORAGE_INSTANCE_PROFILE, payload.getBackupInstanceProfile());
                upgradeRdsService.stopServicesState(payload.getResourceId());
                sendEvent(context, new UpgradeRdsStopServicesRequest(context.getStackId(), context.getVersion()));
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_DATA_BACKUP_STATE")
    public Action<?, ?> backupDataFromRds() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsStopServicesResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsStopServicesResult payload, Map<Object, Object> variables) {
                if (upgradeRdsService.shouldRunDataBackupRestore(context.getStack(), context.getCluster(), context.getDatabase())) {
                    String backupLocation = Objects.toString(variables.get(CLOUD_STORAGE_BACKUP_LOCATION), null);
                    String backupInstanceProfile = Objects.toString(variables.get(CLOUD_STORAGE_INSTANCE_PROFILE), null);
                    upgradeRdsService.backupRdsState(payload.getResourceId());
                    sendEvent(context, new UpgradeRdsDataBackupRequest(context.getStackId(), context.getVersion(), backupLocation, backupInstanceProfile));
                } else {
                    sendEvent(context, new UpgradeRdsDataBackupResult(context.getStackId(), context.getVersion()));
                }
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

    @Bean(name = "UPGRADE_RDS_WAIT_FOR_DATABASE_SERVER_UPGRADE_STATE")
    public Action<?, ?> waitForDatabaseServerUpgrade() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsUpgradeDatabaseServerResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsUpgradeDatabaseServerResult payload, Map<Object, Object> variables) {
                Selectable selectable;
                if (payload.getFlowIdentifier() != null) {
                    selectable = new WaitForDatabaseServerUpgradeRequest(context.getStackId(), context.getVersion(), payload.getFlowIdentifier());
                } else {
                    selectable = new WaitForDatabaseServerUpgradeResult(context.getStackId(), context.getVersion(), payload.getFlowIdentifier());
                }
                sendEvent(context, selectable);
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_MIGRATE_DB_SETTINGS_STATE")
    public Action<?, ?> migrateDatabaseSettings() {
        return new AbstractUpgradeRdsAction<>(WaitForDatabaseServerUpgradeResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, WaitForDatabaseServerUpgradeResult payload, Map<Object, Object> variables) throws Exception {
                if (externalDatabaseService.isMigrationNeededDuringUpgrade(context)) {
                    upgradeRdsService.migrateDatabaseSettingsState(payload.getResourceId());
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeRdsContext context) {
                return externalDatabaseService.isMigrationNeededDuringUpgrade(context) ?
                        new UpgradeRdsMigrateDatabaseSettingsRequest(context.getStackId(), context.getVersion()) :
                        new UpgradeRdsMigrateDatabaseSettingsResponse(context.getStackId(), context.getVersion());
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_DATA_RESTORE_STATE")
    public Action<?, ?> restoreDataToRds() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsMigrateDatabaseSettingsResponse.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsMigrateDatabaseSettingsResponse payload, Map<Object, Object> variables)
                    throws Exception {
                if (upgradeRdsService.shouldRunDataBackupRestore(context.getStack(), context.getCluster(), context.getDatabase())) {
                    upgradeRdsService.restoreRdsState(payload.getResourceId());
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeRdsContext context) {
                return upgradeRdsService.shouldRunDataBackupRestore(context.getStack(), context.getCluster(), context.getDatabase()) ?
                        new UpgradeRdsDataRestoreRequest(context.getStackId(), context.getVersion()) :
                        new UpgradeRdsDataRestoreResult(context.getStackId(), context.getVersion());
            }
        };
    }

    // TODO This is for backward compatibility reason, can be removed in CB-24447
    @Bean(name = "UPGRADE_RDS_START_SERVICES_STATE")
    public Action<?, ?> restartServicesAndCm() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsDataRestoreResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsDataRestoreResult payload, Map<Object, Object> variables) {
                sendEvent(context, new UpgradeRdsDataRestoreResult(context.getStackId(), context.getVersion()));
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_START_CM_STATE")
    public Action<?, ?> restartClusterManager() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsDataRestoreResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsDataRestoreResult payload, Map<Object, Object> variables) {
                upgradeRdsService.startClusterManagerState(payload.getResourceId());
                sendEvent(context, new UpgradeRdsStartCMRequest(context.getStackId(), context.getVersion()));
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_MIGRATE_SERVICES_DB_SETTINGS_STATE")
    public Action<?, ?> migrateServicesDatabaseSettings() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsStartCMResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsStartCMResult payload, Map<Object, Object> variables) {
                if (externalDatabaseService.isMigrationNeededDuringUpgrade(context)) {
                    upgradeRdsService.migrateServicesDatabaseSettingsState(payload.getResourceId());
                }
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeRdsContext context) {
                return externalDatabaseService.isMigrationNeededDuringUpgrade(context) ?
                        new UpgradeRdsMigrateServicesDBSettingsRequest(context.getStackId(), context.getVersion()) :
                        new UpgradeRdsMigrateServicesDBSettingsResponse(context.getStackId(), context.getVersion());
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_START_CMSERVICES_STATE")
    public Action<?, ?> restartCMServices() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsMigrateServicesDBSettingsResponse.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsMigrateServicesDBSettingsResponse payload, Map<Object, Object> variables) {
                upgradeRdsService.startCMServicesState(payload.getResourceId());
                sendEvent(context, new UpgradeRdsStartCMServicesRequest(context.getStackId(), context.getVersion()));
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_INSTALL_POSTGRES_PACKAGES_STATE")
    public Action<?, ?> installPgPackages() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsStartCMServicesResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsStartCMServicesResult payload, Map<Object, Object> variables) {
                upgradeRdsService.installPostgresPackagesState(payload.getResourceId(), payload.getVersion().getMajorVersion());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeRdsContext context) {
                return new UpgradeRdsInstallPostgresPackagesRequest(context.getStackId(), context.getVersion());
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_MIGRATE_ATTACHED_DATAHUBS_STATE")
    public Action<?, ?> migrateAttachedDatahubs() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsInstallPostgresPackagesResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsInstallPostgresPackagesResult payload, Map<Object, Object> variables) {
                Selectable event;
                if (externalDatabaseService.isMigrationNeededDuringUpgrade(context) && upgradeRdsService.shouldMigrateAttachedDatahubs(context.getStack())) {
                    upgradeRdsService.migrateAttachedDatahubs(payload.getResourceId());
                    event = new UpgradeRdsMigrateAttachedDatahubsRequest(context.getStackId(), context.getVersion());
                } else {
                    event = new UpgradeRdsMigrateAttachedDatahubsResponse(context.getStackId(), context.getVersion());
                }
                sendEvent(context, event);
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_VERSION_UPDATE_STATE")
    public Action<?, ?> updateRsdVersion() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsMigrateAttachedDatahubsResponse.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsMigrateAttachedDatahubsResponse payload, Map<Object, Object> variables) {
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(UpgradeRdsContext context) {
                return new UpgradeRdsUpdateVersionRequest(context.getStackId(), context.getVersion());
            }
        };
    }

    @Bean(name = "UPGRADE_RDS_FINISHED_STATE")
    public Action<?, ?> upgradeRdsFinished() {
        return new AbstractUpgradeRdsAction<>(UpgradeRdsUpdateVersionResult.class) {
            @Override
            protected void doExecute(UpgradeRdsContext context, UpgradeRdsUpdateVersionResult payload, Map<Object, Object> variables) {
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
                        Optional.ofNullable(context.getStack()).map(StackView::getClusterId).orElse(null),
                        concretePayload.getException());
                sendEvent(context);
            }

            @Override
            protected Selectable createRequest(StackFailureContext context) {
                return new StackEvent(UpgradeRdsEvent.FAIL_HANDLED_EVENT.event(), context.getStack().getId());
            }
        };
    }
}