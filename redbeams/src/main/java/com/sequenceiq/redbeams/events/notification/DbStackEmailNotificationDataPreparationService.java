package com.sequenceiq.redbeams.events.notification;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.notification.config.CDPConsoleUrlProvider;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.ActionResult;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.ClusterHealthNotificationAdditionalDataDto;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.OperationType;
import com.sequenceiq.redbeams.domain.stack.DBStack;

/**
 * Builds the {@link NotificationGeneratorDtos} payload for the low-storage email alert of an external database, mirroring
 * {@code StackNotificationDataPreparationService} in core. The notification is addressed to the owning cluster CRN (data hub / data lake)
 * so it is delivered through a distribution list where the {@link NotificationType#STACK_HEALTH} event type is registered.
 *
 * <p>The shared {@code stack_health_template.ftl} dereferences {@code ${name}} and the {@code additionalData.results[0]} fields
 * {@code stackType}, {@code operation}, {@code controlPlaneUrl} and {@code dateTimeString} unconditionally; any of them being {@code null}
 * makes FreeMarker throw and the email is silently dropped. All of them are therefore populated with non-null values here.</p>
 */
@Service
public class DbStackEmailNotificationDataPreparationService {

    private static final String DATE_TIME_PATTERN = "MMMM d, yyyy 'at' hh:mm a z";

    private final CDPConsoleUrlProvider cdpConsoleUrlProvider;

    public DbStackEmailNotificationDataPreparationService(CDPConsoleUrlProvider cdpConsoleUrlProvider) {
        this.cdpConsoleUrlProvider = cdpConsoleUrlProvider;
    }

    public NotificationGeneratorDtos notificationGeneratorDtos(
            DBStack dbStack,
            DBSourceClusterDto ownerCluster,
            DBEmailDto dbEmailDto,
            String environmentName,
            CDPStructuredNotificationEvent cdpStructuredNotificationEvent) {
        return NotificationGeneratorDtos.builder()
                .notification(Set.of(prepareResult(dbStack, ownerCluster, dbEmailDto, environmentName, cdpStructuredNotificationEvent)))
                .notificationType(NotificationType.STACK_HEALTH)
                .build();
    }

    /**
     * Prepares a {@link NotificationGeneratorDto} containing notification details for a low-storage email alert
     * of an external database. This method gathers required data from the database stack and owning cluster and
     * formats it according to the notification template.
     *
     * @param dbStack the database stack object containing information about the current database stack
     * @param ownerCluster the cluster that owns the database stack, encapsulating information such as cluster name and CRN
     * @param dbEmailDto the email type describing the status, operation, operation message and main title of the alert
     * @param environmentName the name of the environment where the database stack resides
     * @param cdpStructuredNotificationEvent the stored structured notification event whose status reason is shown in the email
     * @return a {@link NotificationGeneratorDto} object pre-filled with the necessary data for the email alert
     */
    private NotificationGeneratorDto prepareResult(
            DBStack dbStack,
            DBSourceClusterDto ownerCluster,
            DBEmailDto dbEmailDto,
            String environmentName,
            CDPStructuredNotificationEvent cdpStructuredNotificationEvent
            ) {
        return NotificationGeneratorDto.builder()
                .resourceCrn(ownerCluster.sourceClusterCrn())
                .name(ownerCluster.sourceClusterName())
                .resourceName(ownerCluster.sourceClusterName() + "_" + ownerCluster.sourceClusterCrn())
                .severity(NotificationSeverity.ERROR)
                .accountId(dbStack.getAccountId())
                .additionalData(BaseNotificationRegisterAdditionalDataDtos.builder()
                        .results(List.of(ClusterHealthNotificationAdditionalDataDto.builder()
                                .name(ownerCluster.sourceClusterName())
                                .crn(ownerCluster.sourceClusterCrn())
                                .status(dbEmailDto.dbEmailType().status())
                                .stackType(ownerCluster.stackType().equals(StackType.DATALAKE) ? "Data Lake" : "Data Hub")
                                .controlPlaneUrl(cdpConsoleUrlProvider.getEnvironmentUrl(environmentName))
                                .dateTimeString(getCurrentDateTimeForEmail())
                                .statusReason(cdpStructuredNotificationEvent.getStatusReason())
                                .operationMessage(dbEmailDto.dbEmailType().operationMessage())
                                .operationType(OperationType.CHECK)
                                .actionResult(ActionResult.ALERTED)
                                .operation(dbEmailDto.dbEmailType().operation())
                                .mainTitle(dbEmailDto.dbEmailType().mainTitle())
                                .instances(Set.of())
                                .build()))
                        .build())
                .build();
    }

    private String getCurrentDateTimeForEmail() {
        return DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());
    }
}
