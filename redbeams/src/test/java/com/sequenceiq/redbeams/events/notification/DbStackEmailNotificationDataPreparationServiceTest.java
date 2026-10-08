package com.sequenceiq.redbeams.events.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.notification.config.CDPConsoleUrlProvider;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.scheduled.register.dto.BaseNotificationRegisterAdditionalDataDtos;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.ActionResult;
import com.sequenceiq.notification.scheduled.register.dto.clusterhealth.ClusterHealthNotificationAdditionalDataDto;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@ExtendWith(MockitoExtension.class)
class DbStackEmailNotificationDataPreparationServiceTest {

    private static final String DB_CRN = "crn:cdp:redbeams:us-west-1:acc:databaseServer:res";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:acc:cluster:dh";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc:datalake:dl";

    private static final String DB_NAME = "my-external-db";

    private static final String CLUSTER_NAME = "my-cluster";

    private static final String ACCOUNT_ID = "acc";

    private static final String OWNER_CRN = "crn:cdp:iam:us-west-1:acc:user:bob";

    private static final String ENV_NAME = "my-env";

    private static final String CONSOLE_URL = "https://console.example.com/env/my-env";

    private static final String STATUS_REASON = "Only 7.3% storage free";

    @Mock
    private CDPConsoleUrlProvider cdpConsoleUrlProvider;

    @InjectMocks
    private DbStackEmailNotificationDataPreparationService underTest;

    private DBStack dbStack() {
        DBStack dbStack = new DBStack();
        dbStack.setName(DB_NAME);
        dbStack.setResourceCrn(DB_CRN);
        dbStack.setOwnerCrn(Crn.safeFromString(OWNER_CRN));
        return dbStack;
    }

    private DBSourceClusterDto ownerCluster(StackType stackType, String clusterCrn) {
        return new DBSourceClusterDto(CLUSTER_NAME, clusterCrn, stackType, NotificationState.ENABLED);
    }

    private CDPStructuredNotificationEvent event() {
        CDPStructuredNotificationEvent event = Mockito.mock(CDPStructuredNotificationEvent.class);
        when(event.getStatusReason()).thenReturn(STATUS_REASON);
        return event;
    }

    @Test
    void notificationGeneratorDtosShouldBuildStackHealthDtoAddressedToOwningClusterCrn() {
        when(cdpConsoleUrlProvider.getEnvironmentUrl(ENV_NAME)).thenReturn(CONSOLE_URL);

        NotificationGeneratorDtos dtos = underTest.notificationGeneratorDtos(
                dbStack(), ownerCluster(StackType.WORKLOAD, DATAHUB_CRN), DBEmailDto.storageAlert(), ENV_NAME, event());

        assertThat(dtos.getNotificationType()).isEqualTo(NotificationType.STACK_HEALTH);
        assertThat(dtos.getNotifications()).hasSize(1);
        NotificationGeneratorDto dto = (NotificationGeneratorDto) dtos.getNotifications().iterator().next();
        assertThat(dto.getResourceCrn()).isEqualTo(DATAHUB_CRN);
        assertThat(dto.getName()).isEqualTo(CLUSTER_NAME);
        assertThat(dto.getResourceName()).isEqualTo(CLUSTER_NAME + "_" + DATAHUB_CRN);
        assertThat(dto.getSeverity()).isEqualTo(NotificationSeverity.ERROR);
        assertThat(dto.getAccountId()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    void notificationGeneratorDtosShouldPopulateAllTemplateMandatoryFieldsToAvoidSilentDrop() {
        when(cdpConsoleUrlProvider.getEnvironmentUrl(ENV_NAME)).thenReturn(CONSOLE_URL);

        NotificationGeneratorDtos dtos = underTest.notificationGeneratorDtos(
                dbStack(), ownerCluster(StackType.WORKLOAD, DATAHUB_CRN), DBEmailDto.storageAlert(), ENV_NAME, event());

        ClusterHealthNotificationAdditionalDataDto result = firstResult(dtos);
        // the shared stack_health_template.ftl dereferences these unconditionally; a null makes FreeMarker throw and the email is silently dropped
        assertThat(result.getStackType()).isNotNull();
        assertThat(result.getOperation()).isNotNull();
        assertThat(result.getControlPlaneUrl()).isEqualTo(CONSOLE_URL);
        assertThat(result.getDateTimeString()).isNotNull();
        assertThat(result.getName()).isEqualTo(CLUSTER_NAME);
        assertThat(result.getCrn()).isEqualTo(DATAHUB_CRN);
        assertThat(result.getStatusReason()).isEqualTo(STATUS_REASON);
        assertThat(result.getInstances()).isEmpty();
    }

    @Test
    void notificationGeneratorDtosShouldRenderDataHubStackTypeForWorkloadOwner() {
        when(cdpConsoleUrlProvider.getEnvironmentUrl(ENV_NAME)).thenReturn(CONSOLE_URL);

        NotificationGeneratorDtos dtos = underTest.notificationGeneratorDtos(
                dbStack(), ownerCluster(StackType.WORKLOAD, DATAHUB_CRN), DBEmailDto.storageAlert(), ENV_NAME, event());

        assertThat(firstResult(dtos).getStackType()).isEqualTo("Data Hub");
    }

    @Test
    void notificationGeneratorDtosShouldRenderDataLakeStackTypeForDatalakeOwner() {
        when(cdpConsoleUrlProvider.getEnvironmentUrl(ENV_NAME)).thenReturn(CONSOLE_URL);

        NotificationGeneratorDtos dtos = underTest.notificationGeneratorDtos(
                dbStack(), ownerCluster(StackType.DATALAKE, DATALAKE_CRN), DBEmailDto.storageAlert(), ENV_NAME, event());

        assertThat(firstResult(dtos).getStackType()).isEqualTo("Data Lake");
    }

    @Test
    void notificationGeneratorDtosShouldSetAlertedActionResultSoTemplateRendersTheAlertedBranch() {
        when(cdpConsoleUrlProvider.getEnvironmentUrl(ENV_NAME)).thenReturn(CONSOLE_URL);

        NotificationGeneratorDtos dtos = underTest.notificationGeneratorDtos(
                dbStack(), ownerCluster(StackType.WORKLOAD, DATAHUB_CRN), DBEmailDto.storageAlert(), ENV_NAME, event());

        ClusterHealthNotificationAdditionalDataDto result = firstResult(dtos);
        // pins the coupling to the `<#elseif ...actionResult == "alerted">` branch of stack_health_template.ftl: the DTO carries the
        // lowercased ALERTED token the template compares against, plus the operationMessage/mainTitle that branch renders.
        assertThat(result.getActionResult()).isEqualTo(ActionResult.ALERTED.name().toLowerCase(Locale.ROOT));
        assertThat(result.getOperationMessage()).isEqualTo(DBEmailType.EXTERNAL_DATABASE_STORAGE_ALERT.operationMessage());
        assertThat(result.getMainTitle()).isEqualTo(DBEmailType.EXTERNAL_DATABASE_STORAGE_ALERT.mainTitle());
    }

    @SuppressWarnings("unchecked")
    private ClusterHealthNotificationAdditionalDataDto firstResult(NotificationGeneratorDtos dtos) {
        NotificationGeneratorDto dto = (NotificationGeneratorDto) dtos.getNotifications().iterator().next();
        BaseNotificationRegisterAdditionalDataDtos<ClusterHealthNotificationAdditionalDataDto> additionalData =
                (BaseNotificationRegisterAdditionalDataDtos<ClusterHealthNotificationAdditionalDataDto>) dto.getAdditionalData();
        List<ClusterHealthNotificationAdditionalDataDto> results = additionalData.getResults();
        assertThat(results).hasSize(1);
        return results.get(0);
    }
}
