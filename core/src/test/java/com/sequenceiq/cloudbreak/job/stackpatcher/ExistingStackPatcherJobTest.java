package com.sequenceiq.cloudbreak.job.stackpatcher;

import static com.sequenceiq.cloudbreak.job.stackpatcher.ExistingStackPatcherJobAdapter.STACK_PATCH_TYPE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
import com.sequenceiq.cloudbreak.domain.stack.StackPatch;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchApplyException;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchService;
import com.sequenceiq.cloudbreak.service.stackpatch.StackPatchService;

@ExtendWith(MockitoExtension.class)
class ExistingStackPatcherJobTest {

    private static final StackPatchType STACK_PATCH_TYPE = StackPatchType.TEST_PATCH_1;

    @Mock
    private StackService stackService;

    @Mock
    private ExistingStackPatcherJobService jobService;

    @Mock
    private ExistingStackPatchService existingStackPatchService;

    @Mock
    private ExistingStackPatcherServiceProvider existingStackPatcherServiceProvider;

    @Mock
    private StackPatchService stackPatchService;

    @InjectMocks
    private ExistingStackPatcherJob underTest;

    @Captor
    private ArgumentCaptor<JobKey> jobKeyArgumentCaptor;

    @Mock
    private JobExecutionContext context;

    @Mock
    private JobDetail jobDetail;

    @Captor
    private ArgumentCaptor<StackPatch> stackPatchArgumentCaptor;

    private Stack stack;

    private StackPatch stackPatch;

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

        stackPatch = new StackPatch(stack, STACK_PATCH_TYPE);
        lenient().when(stackPatchService.getOrCreate(stack, STACK_PATCH_TYPE)).thenReturn(stackPatch);
    }

    @Test
    void shouldUnscheduleWhenStackIsInFailedOrDeletedStatus() throws JobExecutionException, ExistingStackPatchApplyException {
        setStackStatus(Status.CREATE_FAILED);

        underTest.executeJob(context);

        verifyUnschedule();
        verify(existingStackPatchService, never()).apply(stack);
    }

    @Test
    void shouldNotApplyWhenStackIsAlreadyFixed() throws JobExecutionException, ExistingStackPatchApplyException {
        stackPatch.setStatus(StackPatchStatus.FIXED);

        underTest.executeJob(context);

        verifyUnschedule();
        verify(existingStackPatchService, never()).apply(stack);
    }

    @Test
    void shouldNotApplyWhenStackIsNotAffected() throws JobExecutionException, ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(false);

        underTest.executeJob(context);

        verifyUnschedule();
        verify(existingStackPatchService, never()).apply(stack);
    }

    @Test
    void shouldApplyWhenStackIsAffected() throws JobExecutionException, ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);

        underTest.executeJob(context);

        verify(existingStackPatchService).apply(stack);
        verify(stackPatchService).updateStatusAndReportUsage(stackPatch, StackPatchStatus.AFFECTED);
    }

    @ParameterizedTest
    @EnumSource(value = StackPatchStatus.class, names = {"FAILED", "SKIPPED", "AFFECTED", "UNKNOWN"})
    void shouldApplyButNotReportWhenStackPatchWasAlreadyTried(StackPatchStatus stackPatchStatus) throws Exception {
        stackPatch.setStatus(stackPatchStatus);
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);

        underTest.executeJob(context);

        verify(existingStackPatchService).apply(stack);
        verify(stackPatchService).updateStatus(stackPatch, StackPatchStatus.AFFECTED);
    }

    @Test
    void shouldUnscheduleWhenSuccessfullyApplied() throws JobExecutionException, ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);
        when(existingStackPatchService.apply(stack)).thenReturn(true);

        underTest.executeJob(context);

        verifyUnschedule();
        verify(existingStackPatchService).apply(stack);
        verify(stackPatchService).updateStatusAndReportUsage(stackPatch, StackPatchStatus.AFFECTED);
        verify(stackPatchService).updateStatusAndReportUsage(stackPatch, StackPatchStatus.FIXED);
    }

    @Test
    void shouldNotUnscheduleWhenApplyFails() throws ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);
        String errorMessage = "error message";
        doThrow(new ExistingStackPatchApplyException(errorMessage)).when(existingStackPatchService).apply(stack);

        Assertions.assertThatThrownBy(() -> underTest.executeJob(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageStartingWith("Failed to patch stack");

        verify(jobService, never()).unschedule(any());
        verify(stackPatchService).updateStatusAndReportUsage(stackPatch, StackPatchStatus.AFFECTED);
        verify(stackPatchService).updateStatusAndReportUsage(stackPatch, StackPatchStatus.FAILED, errorMessage);
    }

    @Test
    void shouldFailWhenStackPatcherServiceProviderFails() throws UnknownStackPatchTypeException {
        String stackPatchTypeName = StackPatchType.TEST_PATCH_1.name();
        when(jobDetail.getJobDataMap()).thenReturn(new JobDataMap(Map.of(STACK_PATCH_TYPE_NAME, stackPatchTypeName)));
        doThrow(UnknownStackPatchTypeException.class).when(existingStackPatcherServiceProvider).provide(anyString());

        String errorMessage = "Unknown stack patch type: " + stackPatchTypeName;
        Assertions.assertThatThrownBy(() -> underTest.executeJob(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessage(errorMessage);
        verifyUnschedule();
        verify(stackPatchService).updateStatusAndReportUsage(stackPatchArgumentCaptor.capture(), eq(StackPatchStatus.FAILED), eq(errorMessage));
        Assertions.assertThat(stackPatchArgumentCaptor.getValue())
                .returns(StackPatchType.UNKNOWN, StackPatch::getType)
                .returns(stack, StackPatch::getStack);
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
