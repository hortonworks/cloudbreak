package com.sequenceiq.environment.environment.scheduled.notification.register;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.environment.environment.service.notification.EnvironmentNotificationService;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.DistributionListManagementType;
import com.sequenceiq.notification.domain.NotificationType;
import com.sequenceiq.notification.domain.ResourceSubscription;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDto;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.service.NotificationSendingService;

@ExtendWith(MockitoExtension.class)
class AzureDefaultOutboundNotificationJobTest {

    private static final String RESOURCE_CRN = "crn:cdp:environments:us-west-1:tenant:environment:resource-123";

    private static final String ENV_NAME = "test-env";

    @Mock
    private EnvironmentNotificationService notificationService;

    @Mock
    private TransactionalScheduler scheduler;

    @Mock
    private NotificationSendingService notificationSendingService;

    private AzureDefaultOutboundNotificationJob underTest;

    @BeforeEach
    void setUp() {
        underTest = new AzureDefaultOutboundNotificationJob(
                notificationService,
                scheduler,
                notificationSendingService
        );

        ReflectionTestUtils.setField(underTest, "registerEnabled", true);
    }

    @Test
    void getNameReturnsCorrectJobName() {
        String jobName = ReflectionTestUtils.getField(underTest.getClass(), "JOB_NAME").toString();
        assertEquals("azure-default-outbound-environment-notification", jobName);
    }

    @Test
    void enabledReturnsTrueWhenConfigured() {
        ReflectionTestUtils.setField(underTest, "registerEnabled", true);

        Boolean result = ReflectionTestUtils.invokeMethod(underTest, "enabled");
        assertTrue(result);
    }

    @Test
    void enabledReturnsFalseWhenDisabled() {
        ReflectionTestUtils.setField(underTest, "registerEnabled", false);

        Boolean result = ReflectionTestUtils.invokeMethod(underTest, "enabled");
        assertFalse(result);
    }

    @Test
    void dataReturnsEnvironmentsForOutboundUpgrade() {
        List<NotificationGeneratorDto> expectedData = List.of(
                NotificationGeneratorDto.builder()
                        .resourceCrn(RESOURCE_CRN)
                        .name(ENV_NAME)
                        .build()
        );

        when(notificationService.filterForOutboundUpgradeNotifications()).thenReturn(expectedData);

        Collection<NotificationGeneratorDto> result = ReflectionTestUtils.invokeMethod(underTest, "data");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(notificationService).filterForOutboundUpgradeNotifications();
    }

    @Test
    void notificationTypeReturnsAzureDefaultOutbound() {
        NotificationType result = ReflectionTestUtils.invokeMethod(underTest, "notificationType");

        assertEquals(NotificationType.AZURE_DEFAULT_OUTBOUND, result);
    }

    @Test
    void processNotificationsSendsWithCallback() {
        NotificationGeneratorDtos notificationData = NotificationGeneratorDtos.builder()
                .notification(List.of())
                .notificationType(NotificationType.AZURE_DEFAULT_OUTBOUND)
                .build();

        ReflectionTestUtils.invokeMethod(underTest, "processNotifications", notificationData);

        verify(notificationSendingService).processAndImmediatelySendWithCallback(any(), any());
    }

    @Test
    void onSubscriptionsProcessedWithDistributionLists() {
        DistributionList distributionList1 = DistributionList.builder()
                .externalDistributionListId("dl-1")
                .resourceCrn(RESOURCE_CRN)
                .type(DistributionListManagementType.USER_MANAGED)
                .build();

        DistributionList distributionList2 = DistributionList.builder()
                .externalDistributionListId("dl-2")
                .resourceCrn("crn:cdp:environments:us-west-1:tenant:environment:resource-456")
                .type(DistributionListManagementType.USER_MANAGED)
                .build();

        List<DistributionList> subscriptions = List.of(distributionList1, distributionList2);

        underTest.onSubscriptionsProcessed(subscriptions);

        verify(notificationService).processDistributionListSync(anyList(), anyList());
    }

    @Test
    void onSubscriptionsProcessedWithMixedSubscriptions() {
        DistributionList distributionList = DistributionList.builder()
                .externalDistributionListId("dl-1")
                .resourceCrn(RESOURCE_CRN)
                .type(DistributionListManagementType.USER_MANAGED)
                .build();

        ResourceSubscription resourceSubscription = mock(ResourceSubscription.class);

        List subscriptions = List.of(distributionList, resourceSubscription);

        underTest.onSubscriptionsProcessed(subscriptions);

        verify(notificationService).processDistributionListSync(anyList(), anyList());
    }

    @Test
    void onSubscriptionsProcessedWithEmptyList() {
        List<DistributionList> subscriptions = List.of();

        underTest.onSubscriptionsProcessed(subscriptions);

        verify(notificationService).processDistributionListSync(anyList(), anyList());
    }

    @Test
    void onSubscriptionsProcessedFiltersOnlyDistributionLists() {
        DistributionList distributionList = DistributionList.builder()
                .externalDistributionListId("dl-1")
                .resourceCrn(RESOURCE_CRN)
                .type(DistributionListManagementType.USER_MANAGED)
                .build();

        ResourceSubscription resourceSubscription = mock(ResourceSubscription.class);

        List subscriptions = List.of(distributionList, resourceSubscription, distributionList);

        underTest.onSubscriptionsProcessed(subscriptions);

        verify(notificationService, times(1)).processDistributionListSync(anyList(), anyList());
    }
}