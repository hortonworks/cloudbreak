package com.sequenceiq.freeipa.service.freeipa.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SuccessDetails;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.converter.stack.StackToStackUserSyncViewConverter;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.projection.StackUserSyncView;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.operation.OperationService;

@ExtendWith(MockitoExtension.class)
class PostUserSyncServiceTest {

    private static final String OPERATION_ID = "opId";

    private static final String ACCOUNT_ID = "accId";

    private static final Long TIMEOUT = 6L;

    @Mock
    private TimeoutTaskScheduler timeoutTaskScheduler;

    @Mock
    private ExecutorService usersyncExternalTaskExecutor;

    @Mock
    private OperationService operationService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private SudoRuleService sudoRuleService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private StackToStackUserSyncViewConverter syncViewConverter;

    @InjectMocks
    private PostUserSyncService underTest;

    private Stack stack;

    private StackUserSyncView syncView = mock(StackUserSyncView.class);

    @BeforeEach
    void init() {
        stack = new Stack();
        stack.setEnvironmentCrn("ENV_CRN");
        stack.setAccountId(ACCOUNT_ID);
        lenient().when(syncViewConverter.convert(stack)).thenReturn(syncView);
    }

    @Test
    public void testSchedulingEntitlementPresent() throws Exception {
        Future<Void> task = mock(Future.class);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0, Runnable.class);
            runnable.run();
            return task;
        }).when(usersyncExternalTaskExecutor).submit(any(Runnable.class));
        when(entitlementService.isEnvironmentPrivilegedUserEnabled(ACCOUNT_ID)).thenReturn(true);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        ReflectionTestUtils.setField(underTest, "operationTimeout", TIMEOUT);

        underTest.asyncRunTask(OPERATION_ID, ACCOUNT_ID, stack);

        verify(sudoRuleService).setupSudoRule(syncView, freeIpaClient);
        verify(operationService).completeOperation(ACCOUNT_ID, OPERATION_ID, List.of(new SuccessDetails(stack.getEnvironmentCrn())), List.of());
        verify(timeoutTaskScheduler).scheduleTimeoutTask(OPERATION_ID, ACCOUNT_ID, task, TIMEOUT);
    }

    @Test
    public void testSchedulingEntitlementPresentSudoFails() throws Exception {
        Future<Void> task = mock(Future.class);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0, Runnable.class);
            runnable.run();
            return task;
        }).when(usersyncExternalTaskExecutor).submit(any(Runnable.class));
        when(entitlementService.isEnvironmentPrivilegedUserEnabled(ACCOUNT_ID)).thenReturn(true);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(freeIpaClientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        doThrow(FreeIpaClientException.class).when(sudoRuleService).setupSudoRule(syncView, freeIpaClient);
        ReflectionTestUtils.setField(underTest, "operationTimeout", TIMEOUT);

        underTest.asyncRunTask(OPERATION_ID, ACCOUNT_ID, stack);

        verify(operationService).failOperation(eq(ACCOUNT_ID), eq(OPERATION_ID), anyString());
        verify(timeoutTaskScheduler).scheduleTimeoutTask(OPERATION_ID, ACCOUNT_ID, task, TIMEOUT);
    }

    @Test
    public void testSchedulingEntitlementMissing() {
        Future<Void> task = mock(Future.class);
        doAnswer(inv -> {
            Runnable runnable = inv.getArgument(0, Runnable.class);
            runnable.run();
            return task;
        }).when(usersyncExternalTaskExecutor).submit(any(Runnable.class));
        when(entitlementService.isEnvironmentPrivilegedUserEnabled(ACCOUNT_ID)).thenReturn(false);
        ReflectionTestUtils.setField(underTest, "operationTimeout", TIMEOUT);

        underTest.asyncRunTask(OPERATION_ID, ACCOUNT_ID, stack);

        verify(operationService).completeOperation(ACCOUNT_ID, OPERATION_ID, List.of(new SuccessDetails(stack.getEnvironmentCrn())), List.of());
        verify(timeoutTaskScheduler).scheduleTimeoutTask(OPERATION_ID, ACCOUNT_ID, task, TIMEOUT);
        verifyNoInteractions(sudoRuleService);
    }

}