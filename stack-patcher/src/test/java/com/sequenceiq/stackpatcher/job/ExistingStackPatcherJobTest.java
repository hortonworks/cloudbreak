package com.sequenceiq.stackpatcher.job;

import static com.sequenceiq.stackpatcher.job.ExistingStackPatcherJobAdapter.STACK_PATCH_TYPE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

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
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import com.sequenceiq.stackpatcher.TestStackPatchResource;
import com.sequenceiq.stackpatcher.domain.StackPatch;
import com.sequenceiq.stackpatcher.domain.StackPatchStatus;
import com.sequenceiq.stackpatcher.domain.StackPatchType;
import com.sequenceiq.stackpatcher.service.ExistingStackPatchApplyException;
import com.sequenceiq.stackpatcher.service.ExistingStackPatchService;
import com.sequenceiq.stackpatcher.service.StackPatchResourceService;
import com.sequenceiq.stackpatcher.service.StackPatchService;

@ExtendWith(MockitoExtension.class)
class ExistingStackPatcherJobTest {

    private static final StackPatchType STACK_PATCH_TYPE = StackPatchType.TEST_PATCH_1;

    @Mock
    private StackPatchResourceService<TestStackPatchResource> stackPatchResourceService;

    @Mock
    private ExistingStackPatcherJobService jobService;

    @Mock
    private ExistingStackPatchService<TestStackPatchResource> existingStackPatchService;

    @Mock
    private ExistingStackPatcherServiceProvider existingStackPatcherServiceProvider;

    @Mock
    private StackPatchService stackPatchService;

    @InjectMocks
    private ExistingStackPatcherJob<TestStackPatchResource> underTest;

    @Captor
    private ArgumentCaptor<JobKey> jobKeyArgumentCaptor;

    @Mock
    private JobExecutionContext context;

    @Mock
    private JobDetail jobDetail;

    @Captor
    private ArgumentCaptor<StackPatch> stackPatchArgumentCaptor;

    private TestStackPatchResource stack;

    private StackPatch stackPatch;

    @BeforeEach
    void setUp() throws UnknownStackPatchTypeException {
        stack = new TestStackPatchResource(123L, "stack-crn");

        when(stackPatchResourceService.loadResource(stack.getId())).thenReturn(stack);
        lenient().when(existingStackPatchService.getStackPatchType()).thenReturn(STACK_PATCH_TYPE);
        lenient().doReturn(existingStackPatchService).when(existingStackPatcherServiceProvider).provide(anyString());

        underTest.setLocalId(stack.getId().toString());
        underTest.setRemoteResourceCrn(stack.getResourceCrn());

        lenient().when(jobDetail.getKey()).thenReturn(JobKey.jobKey(stack.getId().toString()));
        lenient().when(jobDetail.getJobDataMap()).thenReturn(new JobDataMap(Map.of(STACK_PATCH_TYPE_NAME, STACK_PATCH_TYPE.name())));
        lenient().when(context.getJobDetail()).thenReturn(jobDetail);

        stackPatch = new StackPatch(stack.getId(), STACK_PATCH_TYPE);
        lenient().when(stackPatchService.getOrCreate(stack.getId(), STACK_PATCH_TYPE)).thenReturn(stackPatch);
    }

    @Test
    void shouldUnscheduleWhenStackIsUnschedulable() throws JobExecutionException, ExistingStackPatchApplyException {
        when(stackPatchResourceService.isUnschedulable(stack)).thenReturn(true);
        when(stackPatchService.find(stack.getId(), STACK_PATCH_TYPE)).thenReturn(Optional.of(stackPatch));

        underTest.executeJob(context);

        verifyUnschedule();
        verify(existingStackPatchService, never()).apply(stack);
        verify(stackPatchService, never()).getOrCreate(any(Long.class), any());
        verify(stackPatchService).updateStatus(stackPatch, StackPatchStatus.UNSCHEDULED);
    }

    @Test
    void shouldUnscheduleWithoutCreatingStackPatchWhenStackIsUnschedulableAndHasNoStackPatch() throws JobExecutionException {
        when(stackPatchResourceService.isUnschedulable(stack)).thenReturn(true);
        when(stackPatchService.find(stack.getId(), STACK_PATCH_TYPE)).thenReturn(Optional.empty());

        underTest.executeJob(context);

        verifyUnschedule();
        verify(stackPatchService, never()).getOrCreate(any(Long.class), any());
        verify(stackPatchService, never()).updateStatus(any(), any());
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
        verify(existingStackPatchService, never()).isEntitled(stack);
        verify(existingStackPatchService, never()).apply(stack);
        verify(stackPatchService).updateStatus(stackPatch, StackPatchStatus.NOT_AFFECTED);
    }

    @Test
    void shouldSkipAndRemainScheduledWhenStackIsAffectedButNotEntitled() throws JobExecutionException, ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);
        when(existingStackPatchService.isEntitled(stack)).thenReturn(false);

        underTest.executeJob(context);

        verify(jobService, never()).unschedule(any());
        verify(existingStackPatchService, never()).apply(stack);
        verify(stackPatchService).updateStatus(stackPatch, StackPatchStatus.SKIPPED);
        verify(stackPatchService, never()).updateStatusAndNotify(stackPatch, stack, StackPatchStatus.AFFECTED);
    }

    @Test
    void shouldApplyWhenStackIsAffected() throws JobExecutionException, ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);
        when(existingStackPatchService.isEntitled(stack)).thenReturn(true);

        underTest.executeJob(context);

        verify(existingStackPatchService).apply(stack);
        verify(stackPatchService).updateStatusAndNotify(stackPatch, stack, StackPatchStatus.AFFECTED);
    }

    @ParameterizedTest
    @EnumSource(value = StackPatchStatus.class, names = {"FAILED", "SKIPPED", "AFFECTED", "UNKNOWN"})
    void shouldApplyButNotReportWhenStackPatchWasAlreadyTried(StackPatchStatus stackPatchStatus) throws Exception {
        stackPatch.setStatus(stackPatchStatus);
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);
        when(existingStackPatchService.isEntitled(stack)).thenReturn(true);

        underTest.executeJob(context);

        verify(existingStackPatchService).apply(stack);
        verify(stackPatchService).updateStatus(stackPatch, StackPatchStatus.AFFECTED);
        verify(stackPatchService, never()).updateStatusAndNotify(stackPatch, stack, StackPatchStatus.AFFECTED);
    }

    @Test
    void shouldUnscheduleWhenSuccessfullyApplied() throws JobExecutionException, ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);
        when(existingStackPatchService.isEntitled(stack)).thenReturn(true);
        when(existingStackPatchService.apply(stack)).thenReturn(true);

        underTest.executeJob(context);

        verifyUnschedule();
        verify(existingStackPatchService).apply(stack);
        verify(stackPatchService).updateStatusAndNotify(stackPatch, stack, StackPatchStatus.AFFECTED);
        verify(stackPatchService).updateStatusAndNotify(stackPatch, stack, StackPatchStatus.FIXED);
    }

    @Test
    void shouldNotUnscheduleWhenApplyFails() throws ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);
        when(existingStackPatchService.isEntitled(stack)).thenReturn(true);
        String errorMessage = "error message";
        doThrow(new ExistingStackPatchApplyException(errorMessage)).when(existingStackPatchService).apply(stack);

        Assertions.assertThatThrownBy(() -> underTest.executeJob(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageStartingWith("Failed to patch stack");

        verify(jobService, never()).unschedule(any());
        verify(stackPatchService).updateStatusAndNotify(stackPatch, stack, StackPatchStatus.AFFECTED);
        verify(stackPatchService).updateStatusAndNotify(stackPatch, stack, StackPatchStatus.FAILED, errorMessage);
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
        verify(stackPatchService).updateStatusAndNotify(stackPatchArgumentCaptor.capture(), eq(stack), eq(StackPatchStatus.FAILED), eq(errorMessage));
        Assertions.assertThat(stackPatchArgumentCaptor.getValue())
                .returns(StackPatchType.UNKNOWN, StackPatch::getType)
                .returns(stack.getId(), StackPatch::getResourceId);
    }

    private void verifyUnschedule() {
        verify(jobService).unschedule(jobKeyArgumentCaptor.capture());
        Assertions.assertThat(jobKeyArgumentCaptor.getValue())
                .returns(underTest.getLocalId(), JobKey::getName);
    }

}
