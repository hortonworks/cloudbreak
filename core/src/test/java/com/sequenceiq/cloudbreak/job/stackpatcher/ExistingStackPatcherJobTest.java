package com.sequenceiq.cloudbreak.job.stackpatcher;

import static com.sequenceiq.cloudbreak.job.stackpatcher.ExistingStackPatcherJobAdapter.STACK_PATCH_TYPE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchApplyException;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchService;
import com.sequenceiq.cloudbreak.service.stackpatch.StackPatchUsageReporterService;

@ExtendWith(MockitoExtension.class)
class ExistingStackPatcherJobTest {

    private static final StackPatchType STACK_PATCH_TYPE = StackPatchType.UNBOUND_RESTART;

    @Mock
    private StackService stackService;

    @Mock
    private ExistingStackPatcherJobService jobService;

    @Mock
    private ExistingStackPatchService existingStackPatchService;

    @Mock
    private StackPatchUsageReporterService stackPatchUsageReporterService;

    @Mock
    private ExistingStackPatcherServiceProvider existingStackPatcherServiceProvider;

    @InjectMocks
    private ExistingStackPatcherJob underTest;

    @Captor
    private ArgumentCaptor<JobKey> jobKeyArgumentCaptor;

    @Mock
    private JobExecutionContext context;

    @Mock
    private JobDetail jobDetail;

    private Stack stack;

    @BeforeEach
    void setUp() throws UnknownStackPatchTypeException {
        stack = new Stack();
        stack.setId(123L);
        stack.setResourceCrn("stack-crn");
        setStackStatus(Status.AVAILABLE);

        MockitoAnnotations.openMocks(this);

        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);
        lenient().when(existingStackPatchService.getStackPatchType()).thenReturn(STACK_PATCH_TYPE);
        lenient().doReturn(existingStackPatchService).when(existingStackPatcherServiceProvider).provide(anyString());

        underTest.setLocalId(stack.getId().toString());
        underTest.setRemoteResourceCrn(stack.getResourceCrn());

        lenient().when(jobDetail.getKey()).thenReturn(JobKey.jobKey(stack.getId().toString()));
        lenient().when(jobDetail.getJobDataMap()).thenReturn(new JobDataMap(Map.of(STACK_PATCH_TYPE_NAME, STACK_PATCH_TYPE.name())));
        lenient().when(context.getJobDetail()).thenReturn(jobDetail);
    }

    @Test
    void shouldUnscheduleWhenStackIsInFailedOrDeletedStatus() throws JobExecutionException, ExistingStackPatchApplyException {
        setStackStatus(Status.CREATE_FAILED);

        underTest.executeTracedJob(context);

        verifyUnschedule();
        verify(existingStackPatchService, never()).apply(stack);
    }

    @Test
    void shouldNotApplyWhenStackIsAlreadyFixed() throws JobExecutionException, ExistingStackPatchApplyException {
        when(existingStackPatchService.isStackAlreadyFixed(stack)).thenReturn(true);

        underTest.executeTracedJob(context);

        verifyUnschedule();
        verify(existingStackPatchService, never()).apply(stack);
    }

    @Test
    void shouldNotApplyWhenStackIsNotAffected() throws JobExecutionException, ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(false);

        underTest.executeTracedJob(context);

        verifyUnschedule();
        verify(existingStackPatchService, never()).apply(stack);
    }

    @Test
    void shouldApplyWhenStackIsAffected() throws JobExecutionException, ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);

        underTest.executeTracedJob(context);

        verify(existingStackPatchService).apply(stack);
        verify(stackPatchUsageReporterService).reportAffected(stack, STACK_PATCH_TYPE);
    }

    @Test
    void shouldUnscheduleWhenSuccessfullyApplied() throws JobExecutionException, ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);
        when(existingStackPatchService.apply(stack)).thenReturn(true);

        underTest.executeTracedJob(context);

        verifyUnschedule();
        verify(existingStackPatchService).apply(stack);
        verify(stackPatchUsageReporterService).reportAffected(stack, STACK_PATCH_TYPE);
        verify(stackPatchUsageReporterService).reportSuccess(stack, STACK_PATCH_TYPE);
    }

    @Test
    void shouldNotUnscheduleWhenApplyFails() throws ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);
        String errorMessage = "error message";
        doThrow(new ExistingStackPatchApplyException(errorMessage)).when(existingStackPatchService).apply(stack);

        Assertions.assertThatThrownBy(() -> underTest.executeTracedJob(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageStartingWith("Failed to patch stack");

        verify(jobService, never()).unschedule(any());
        verify(stackPatchUsageReporterService).reportAffected(stack, STACK_PATCH_TYPE);
        verify(stackPatchUsageReporterService).reportFailure(stack, STACK_PATCH_TYPE, errorMessage);
    }

    @Test
    void shouldFailWhenStackPatcherServiceProviderFails() throws UnknownStackPatchTypeException {
        String stackPatchTypeName = StackPatchType.LOGGING_AGENT_AUTO_RESTART.name();
        when(jobDetail.getJobDataMap()).thenReturn(new JobDataMap(Map.of(STACK_PATCH_TYPE_NAME, stackPatchTypeName)));
        doThrow(UnknownStackPatchTypeException.class).when(existingStackPatcherServiceProvider).provide(anyString());

        String errorMessage = "Unknown stack patch type: " + stackPatchTypeName;
        Assertions.assertThatThrownBy(() -> underTest.executeTracedJob(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessage(errorMessage);
        verifyUnschedule();
        verify(stackPatchUsageReporterService).reportFailure(stack, StackPatchType.UNKNOWN, errorMessage);
    }

    private void setStackStatus(Status status) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        stack.setStackStatus(stackStatus);
    }

    private void verifyUnschedule() {
        verify(jobService).unschedule(jobKeyArgumentCaptor.capture());
        Assertions.assertThat(jobKeyArgumentCaptor.getValue())
                .returns(underTest.getLocalId(), JobKey::getName);
    }

}
