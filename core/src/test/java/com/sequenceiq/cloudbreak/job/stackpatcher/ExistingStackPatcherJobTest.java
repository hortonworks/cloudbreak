package com.sequenceiq.cloudbreak.job.stackpatcher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

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
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchApplyException;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ExistingStackPatcherJobTest {

    @Mock
    private StackService stackService;

    @Mock
    private ExistingStackPatcherJobService jobService;

    @Mock
    private ExistingStackPatchService existingStackPatchService;

    @InjectMocks
    private ExistingStackPatcherJob underTest;

    @Captor
    private ArgumentCaptor<JobKey> jobKeyArgumentCaptor;

    @Mock
    private JobExecutionContext context;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(123L);
        stack.setResourceCrn("stack-crn");
        setStackStatus(Status.AVAILABLE);

        MockitoAnnotations.openMocks(this);

        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);

        underTest.setLocalId(stack.getId().toString());
        underTest.setRemoteResourceCrn(stack.getResourceCrn());

        setStackFixServices();

        JobDetail jobDetail = mock(JobDetail.class);
        lenient().when(jobDetail.getKey()).thenReturn(JobKey.jobKey(stack.getId().toString()));
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

        verifyUnschedule();
        verify(existingStackPatchService).apply(stack);
    }

    @Test
    void shouldNotUnscheduleWhenApplyFails() throws ExistingStackPatchApplyException {
        when(existingStackPatchService.isAffected(stack)).thenReturn(true);
        doThrow(ExistingStackPatchApplyException.class).when(existingStackPatchService).apply(stack);

        Assertions.assertThatThrownBy(() -> underTest.executeTracedJob(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageStartingWith("Failed to patch stack");

        verify(jobService, never()).unschedule(any());
    }

    @Test
    void shouldNotUnscheduleWhenOneApplyFailsAndOneSucceeds() throws ExistingStackPatchApplyException {
        ExistingStackPatchService failingExistingStackPatchService = mock(ExistingStackPatchService.class);
        when(failingExistingStackPatchService.isAffected(stack)).thenReturn(true);
        doThrow(ExistingStackPatchApplyException.class).when(failingExistingStackPatchService).apply(stack);
        setStackFixServices(failingExistingStackPatchService);

        when(existingStackPatchService.isAffected(stack)).thenReturn(true);

        Assertions.assertThatThrownBy(() -> underTest.executeTracedJob(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageStartingWith("Failed to patch stack");

        verify(jobService, never()).unschedule(any());
    }

    private void setStackStatus(Status status) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        stack.setStackStatus(stackStatus);
    }

    /**
     * workaround for collection injection
     */
    private void setStackFixServices(ExistingStackPatchService... additionalServices) {
        try {
            Field existingStackPatchServicesField = ExistingStackPatcherJob.class.getDeclaredField("existingStackPatchServices");
            ReflectionUtils.makeAccessible(existingStackPatchServicesField);
            Set<ExistingStackPatchService> existingStackPatchServices = new HashSet<>();
            existingStackPatchServices.add(existingStackPatchService);
            existingStackPatchServices.addAll(Set.of(additionalServices));
            ReflectionUtils.setField(existingStackPatchServicesField, underTest, existingStackPatchServices);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private void verifyUnschedule() {
        verify(jobService).unschedule(jobKeyArgumentCaptor.capture());
        Assertions.assertThat(jobKeyArgumentCaptor.getValue())
                .returns(underTest.getLocalId(), JobKey::getName);
    }

}
