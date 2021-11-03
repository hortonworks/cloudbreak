package com.sequenceiq.cloudbreak.job.existingstackfix;

import static org.mockito.Mockito.doThrow;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobExecutionException;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.existingstackfix.ExistingStackFixService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ExistingStackFixerJobTest {

    @Mock
    private StackService stackService;

    @Mock
    private ExistingStackFixerJobService jobService;

    @Mock
    private ExistingStackFixService existingStackFixService;

    @InjectMocks
    private ExistingStackFixerJob underTest;

    private Stack stack;

    @BeforeEach
    void setUp() throws NoSuchFieldException {
        stack = new Stack();
        stack.setId(123L);
        stack.setResourceCrn("stack-crn");
        setStackStatus(Status.AVAILABLE);

        MockitoAnnotations.openMocks(this);

        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);

        underTest.setLocalId(stack.getId().toString());
        underTest.setRemoteResourceCrn(stack.getResourceCrn());

        setStackFixServices();
    }

    @Test
    void shouldUnscheduleWhenStackIsInFailedOrDeletedStatus() throws JobExecutionException {
        setStackStatus(Status.CREATE_FAILED);

        underTest.executeTracedJob(null);

        verify(jobService).unschedule(underTest.getLocalId());
        verify(existingStackFixService, never()).apply(stack);
    }

    @Test
    void shouldNotApplyWhenStackIsAlreadyFixed() throws JobExecutionException {
        when(existingStackFixService.isStackAlreadyFixed(stack)).thenReturn(true);

        underTest.executeTracedJob(null);

        verify(jobService).unschedule(underTest.getLocalId());
        verify(existingStackFixService, never()).apply(stack);
    }

    @Test
    void shouldNotApplyWhenStackIsNotAffected() throws JobExecutionException {
        when(existingStackFixService.isAffected(stack)).thenReturn(false);

        underTest.executeTracedJob(null);

        verify(jobService).unschedule(underTest.getLocalId());
        verify(existingStackFixService, never()).apply(stack);
    }

    @Test
    void shouldApplyWhenStackIsAffected() throws JobExecutionException {
        when(existingStackFixService.isAffected(stack)).thenReturn(true);

        underTest.executeTracedJob(null);

        verify(jobService).unschedule(underTest.getLocalId());
        verify(existingStackFixService).apply(stack);
    }

    @Test
    void shouldNotUnscheduleWhenApplyFails() {
        when(existingStackFixService.isAffected(stack)).thenReturn(true);
        doThrow(RuntimeException.class).when(existingStackFixService).apply(stack);

        Assertions.assertThatThrownBy(() -> underTest.executeTracedJob(null))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageStartingWith("Failed to fix stack");

        verify(jobService, never()).unschedule(underTest.getLocalId());
    }

    @Test
    void shouldNotUnscheduleWhenOneApplyFailsAndOneSucceeds() {
        ExistingStackFixService failingExistingStackFixService = mock(ExistingStackFixService.class);
        when(failingExistingStackFixService.isAffected(stack)).thenReturn(true);
        doThrow(RuntimeException.class).when(failingExistingStackFixService).apply(stack);
        setStackFixServices(failingExistingStackFixService);

        when(existingStackFixService.isAffected(stack)).thenReturn(true);

        Assertions.assertThatThrownBy(() -> underTest.executeTracedJob(null))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageStartingWith("Failed to fix stack");

        verify(jobService, never()).unschedule(underTest.getLocalId());
    }

    private void setStackStatus(Status status) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        stack.setStackStatus(stackStatus);
    }

    /**
     * workaround for collection injection
     */
    private void setStackFixServices(ExistingStackFixService... additionalServices) {
        try {
            Field existingStackFixServicesField = ExistingStackFixerJob.class.getDeclaredField("existingStackFixServices");
            ReflectionUtils.makeAccessible(existingStackFixServicesField);
            Set<ExistingStackFixService> existingStackFixServices = new HashSet<>();
            existingStackFixServices.add(existingStackFixService);
            existingStackFixServices.addAll(Set.of(additionalServices));
            ReflectionUtils.setField(existingStackFixServicesField, underTest, existingStackFixServices);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

}
