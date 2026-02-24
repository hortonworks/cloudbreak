package com.sequenceiq.cloudbreak.service.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.notification.NotificationState;
import com.sequenceiq.cloudbreak.conf.EnvironmentInternalClientConfiguration;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentInternalEndpoint;
import com.sequenceiq.environment.client.EnvironmentInternalCrnClient;
import com.sequenceiq.environment.client.EnvironmentServiceCrnEndpoints;
import com.sequenceiq.notification.generator.dto.NotificationGeneratorDtos;
import com.sequenceiq.notification.service.NotificationSendingService;

@ExtendWith(MockitoExtension.class)
class StackNotificationServiceTest {

    private static final String TEST_ACCOUNT_ID = "test-account";

    private static final String TEST_CRN = "crn:cdp:datahub:us-west-1:test-account:cluster:cbcd0f38-b026-4116-97d3-23a041faa3f8";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private NotificationSendingService notificationSendingService;

    @Mock
    private StackNotificationDataPreparationService stackNotificationDataPreparationService;

    @Mock
    private StackNotificationTypePreparationService stackNotificationTypePreparationService;

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Mock
    private EnvironmentInternalClientConfiguration environmentInternalClientConfiguration;

    @Mock
    private Future<Boolean> future;

    @InjectMocks
    private StackNotificationService underTest;

    void setUpExecutor() {
        // Mock the executor to run tasks synchronously for testing
        when(intermediateBuilderExecutor.submit(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Boolean> callable = invocation.getArgument(0);
            try {
                callable.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return future;
        });
    }

    @Test
    void testNotifyWhenAllConditionsMet() throws Exception {
        setUpExecutor();
        Stack stack = createTestStack();
        Status newStatus = Status.CREATE_FAILED;
        DetailedStackStatus newDetailedStatus = DetailedStackStatus.PROVISION_FAILED;
        String statusReason = "Test failure";
        NotificationGeneratorDtos notificationGeneratorDtos = NotificationGeneratorDtos.builder().build();
        EnvironmentInternalCrnClient environmentInternalCrnClient = mock(EnvironmentInternalCrnClient.class);
        EnvironmentServiceCrnEndpoints environmentServiceCrnEndpoints = mock(EnvironmentServiceCrnEndpoints.class);
        EnvironmentInternalEndpoint environmentInternalEndpoint = mock(EnvironmentInternalEndpoint.class);

        doNothing().when(environmentInternalEndpoint).createOrUpdateDistributionListByEnvironmentCrn(anyString());
        when(environmentServiceCrnEndpoints.environmentInternalEndpoint()).thenReturn(environmentInternalEndpoint);
        when(environmentInternalCrnClient.withInternalCrn()).thenReturn(environmentServiceCrnEndpoints);
        when(environmentInternalClientConfiguration.cloudbreakInternalCrnClientClient()).thenReturn(environmentInternalCrnClient);
        when(stackNotificationTypePreparationService.isNotificationRequiredByStackStatus(newStatus)).thenReturn(true);
        when(entitlementService.isCdpCbNotificationSendingEnabled(TEST_ACCOUNT_ID)).thenReturn(true);
        when(entitlementService.isCdpCbNotificationSendingEnabled(TEST_ACCOUNT_ID)).thenReturn(true);
        when(stackNotificationDataPreparationService.notificationGeneratorDtos(stack, newStatus, newDetailedStatus, statusReason, TEST_ACCOUNT_ID))
                .thenReturn(notificationGeneratorDtos);
        when(future.get()).thenReturn(true);

        underTest.notify(stack, newStatus, newDetailedStatus, statusReason);

        verify(notificationSendingService).processAndImmediatelySend(notificationGeneratorDtos);
    }

    @Test
    void testNotifyWhenNotificationNotRequiredByStatus() {
        Stack stack = createTestStack();
        Status newStatus = Status.AVAILABLE;

        when(stackNotificationTypePreparationService.isNotificationRequiredByStackStatus(newStatus)).thenReturn(false);

        underTest.notify(stack, newStatus, DetailedStackStatus.AVAILABLE, "All good");

        verify(notificationSendingService, never()).processAndImmediatelySend(any());
    }

    @Test
    void testNotifyWhenCbNotificationSendingDisabled() {
        Stack stack = createTestStack();
        Status newStatus = Status.CREATE_FAILED;

        when(stackNotificationTypePreparationService.isNotificationRequiredByStackStatus(newStatus)).thenReturn(true);
        when(entitlementService.isCdpCbNotificationSendingEnabled(TEST_ACCOUNT_ID)).thenReturn(false);

        underTest.notify(stack, newStatus, DetailedStackStatus.PROVISION_FAILED, "Test failure");

        verify(notificationSendingService, never()).processAndImmediatelySend(any());
    }

    @Test
    void testNotifyWhenStackFailureNotificationSendingDisabled() {
        Stack stack = createTestStack();
        Status newStatus = Status.CREATE_FAILED;

        when(stackNotificationTypePreparationService.isNotificationRequiredByStackStatus(newStatus)).thenReturn(true);
        when(entitlementService.isCdpCbNotificationSendingEnabled(TEST_ACCOUNT_ID)).thenReturn(false);

        underTest.notify(stack, newStatus, DetailedStackStatus.PROVISION_FAILED, "Test failure");

        verify(notificationSendingService, never()).processAndImmediatelySend(any());
    }

    private Stack createTestStack() {
        Stack stack = new Stack();
        stack.setResourceCrn(TEST_CRN);
        stack.setName("test-stack");
        stack.setEnvironmentCrn("envcrn");
        stack.setNotificationState(NotificationState.ENABLED);
        return stack;
    }
}
