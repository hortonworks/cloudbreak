package com.sequenceiq.redbeams.events.notification;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.notification.domain.DistributionListActionType;
import com.sequenceiq.notification.domain.NotificationGroupType;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.sender.DistributionListManagementService;
import com.sequenceiq.notification.service.NotificationSendingService;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.service.EnvironmentService;

/**
 * Sends the low-storage email alert for an external database through the Notification Service, mirroring the delivery core performs via
 * {@code StackNotificationService.notify}. As in core, the registration + send is dispatched on the {@code intermediateBuilderExecutor} pool
 * (not the caller's storage-sync thread); the caller blocks on the returned {@link Future}, bounded by a timeout, only to log the outcome.
 * redbeams has no logged-in user on its background poller threads, so the whole flow runs as the internal actor
 * ({@link ThreadBasedUserCrnProvider#doAsInternalActor}).
 *
 * <p>The email is addressed to the owning data hub / data lake cluster CRN (resolved from {@link DatabaseServerConfig#getClusterCrn()}), with the
 * environment as the distribution-list parent, because {@link NotificationGroupType#byCrn} maps the database-server CRN to {@code ENVIRONMENT} where
 * {@code STACK_HEALTH} is not registered. If the database has no owning cluster CRN, the email cannot be routed and is skipped.</p>
 *
 * <p>Any failure is logged and swallowed: a notification problem must never break the storage-sync flow that triggered it.</p>
 */
@Service
public class DbStackEmailNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbStackEmailNotificationService.class);

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private DistributionListManagementService distributionListManagementService;

    @Inject
    private NotificationSendingService notificationSendingService;

    @Inject
    private DbStackEmailNotificationDataPreparationService dataPreparationService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private DbStackNotificationTypePreparationService dbStackNotificationTypePreparationService;

    @Inject
    private DBStackOwnerIdentifierService dbStackOwnerIdentifierService;

    @Inject
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Value("${redbeams.storage-alert.email.timeout-seconds:60}")
    private long emailTimeoutSeconds;

    public void notify(
            DatabaseServerConfig databaseServerConfig,
            DBStack dbStack,
            DBEmailDto dbEmailDto,
            CDPStructuredNotificationEvent cdpStructuredNotificationEvent
    ) {
        String clusterCrn = databaseServerConfig.getClusterCrn();
        ResourceEvent resourceEvent = cdpStructuredNotificationEvent.getNotificationDetails().getResourceEvent();
        String accountId = Crn.fromString(dbStack.getResourceCrn()).getAccountId();
        DBSourceClusterDto ownerCluster = dbStackOwnerIdentifierService.getOwningClusterNotificationState(clusterCrn);
        if (shouldSendNotification(resourceEvent, ownerCluster, accountId)) {
            Future<Boolean> future = sendNotification(
                    dbStack,
                    ownerCluster,
                    dbEmailDto,
                    cdpStructuredNotificationEvent
            );
            try {
                Boolean success = future.get(emailTimeoutSeconds, TimeUnit.SECONDS);
                if (success) {
                    LOGGER.info("Notification was sent about Database health change for cluster: {}", dbStack.getName());
                } else {
                    LOGGER.info("Notification was not sent about Database health change for cluster: {}", dbStack.getName());
                }
            } catch (ExecutionException e) {
                LOGGER.error("Notification could not be sent for database {} because {}.", dbStack.getName(), e.getMessage(), e);
            } catch (TimeoutException e) {
                future.cancel(true);
                LOGGER.error("Notification sending timed out after {} seconds for database {}.", emailTimeoutSeconds, dbStack.getName(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Notification sending was interrupted for database {} because {}.", dbStack.getName(), e.getMessage(), e);
            }
        } else {
            LOGGER.info("Notification was not sent about Database health change for database: {} status: {} account id {}",
                    dbStack.getName(), resourceEvent, accountId);
        }
    }

    private boolean shouldSendNotification(ResourceEvent resourceEvent, DBSourceClusterDto ownerCluster, String accountId) {
        boolean notificationRequiredByStackStatus = dbStackNotificationTypePreparationService.isNotificationRequiredByStackStatus(resourceEvent);
        boolean cdpCbNotificationSendingEnabled = entitlementService.isCdpCbNotificationSendingEnabled(accountId);
        boolean enabledNotificationForCluster = NotificationState.ENABLED.equals(ownerCluster.notificationState());
        LOGGER.info("Notification sending calculation " +
                        "notificationRequiredByStackStatus: {} " +
                        "cdpCbNotificationSendingEnabled: {} " +
                        "enabledNotificationForCluster: {}.",
                notificationRequiredByStackStatus, cdpCbNotificationSendingEnabled, enabledNotificationForCluster);
        return notificationRequiredByStackStatus && cdpCbNotificationSendingEnabled && enabledNotificationForCluster;
    }

    private Future<Boolean> sendNotification(
            DBStack dbStack,
            DBSourceClusterDto ownerCluster,
            DBEmailDto dbEmailDto,
            CDPStructuredNotificationEvent cdpStructuredNotificationEvent) {
        String environmentCrn = dbStack.getEnvironmentId();
        return intermediateBuilderExecutor.submit(() -> {
            try {
                return ThreadBasedUserCrnProvider.doAsInternalActor(() -> {
                    String environmentName = environmentService.getByCrn(environmentCrn).getName();
                    distributionListManagementService.createOrUpdateList(
                            environmentCrn,
                            environmentName,
                            ownerCluster.sourceClusterName(),
                            ownerCluster.sourceClusterCrn(),
                            NotificationGroupType.byCrn(ownerCluster.sourceClusterCrn()),
                            NotificationSeverity.ERROR,
                            DistributionListActionType.REGISTRATION);
                    NotificationGeneratorDtos dtos = dataPreparationService.notificationGeneratorDtos(
                            dbStack,
                            ownerCluster,
                            dbEmailDto,
                            environmentName,
                            cdpStructuredNotificationEvent
                    );
                    notificationSendingService.processAndImmediatelySend(dtos);
                    return true;
                });
            } catch (Exception e) {
                LOGGER.warn("Failed to send low-storage email notification for external database {}: {}", dbStack.getResourceCrn(), e.getMessage(), e);
                throw e;
            }
        });
    }
}
