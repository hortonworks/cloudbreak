package com.sequenceiq.cloudbreak.job.stackpatcher;

import static com.sequenceiq.cloudbreak.job.stackpatcher.ExistingStackPatcherJobAdapter.STACK_PATCH_TYPE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
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
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.converter.StackPatchTypeConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchApplyException;
import com.sequenceiq.cloudbreak.service.stackpatch.ExistingStackPatchService;

@ExtendWith(MockitoExtension.class)
class ExistingStackPatcherJobTest {

    @Mock
    private StackService stackService;

    @Mock
    private ExistingStackPatcherJobService jobService;

    @Mock
    private ExistingStackPatchService existingStackPatchService;

    @Mock
    private StackPatchTypeConverter stackPatchTypeConverter;

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
    void setUp() {
        stack = new Stack();
        stack.setId(123L);
        stack.setResourceCrn("stack-crn");
        setStackStatus(Status.AVAILABLE);

        MockitoAnnotations.openMocks(this);

        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);
        lenient().when(existingStackPatchService.getStackPatchType()).thenReturn(StackPatchType.UNBOUND_RESTART);
        lenient().when(stackPatchTypeConverter.convertToEntityAttribute(any())).thenReturn(StackPatchType.UNBOUND_RESTART);

        underTest.setLocalId(stack.getId().toString());
        underTest.setRemoteResourceCrn(stack.getResourceCrn());

        setStackPatchServices();

        lenient().when(jobDetail.getKey()).thenReturn(JobKey.jobKey(stack.getId().toString()));
        lenient().when(jobDetail.getJobDataMap()).thenReturn(new JobDataMap(Map.of(STACK_PATCH_TYPE_NAME, "UNBOUND_RESTART")));
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
    void shouldFailWhenStackPatchTypeIsNull() {
        when(jobDetail.getJobDataMap()).thenReturn(new JobDataMap(Map.of()));
        when(stackPatchTypeConverter.convertToEntityAttribute(any())).thenReturn(null);

        Assertions.assertThatThrownBy(() -> underTest.executeTracedJob(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageStartingWith("Stack patch type null is unknown");
        verifyUnschedule();
    }

    @Test
    void shouldFailWhenStackPatchTypeIsUnknown() {
        when(jobDetail.getJobDataMap()).thenReturn(new JobDataMap(Map.of(STACK_PATCH_TYPE_NAME, "TEST_UNKNOWN_TYPE")));
        when(stackPatchTypeConverter.convertToEntityAttribute(any())).thenReturn(StackPatchType.UNKNOWN);

        Assertions.assertThatThrownBy(() -> underTest.executeTracedJob(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageStartingWith("Stack patch type TEST_UNKNOWN_TYPE is unknown");
        verifyUnschedule();
    }

    @Test
    void shouldFailWhenStackPatchTypeDoesNotHaveService() {
        when(stackPatchTypeConverter.convertToEntityAttribute(any())).thenReturn(StackPatchType.LOGGING_AGENT_AUTO_RESTART);

        Assertions.assertThatThrownBy(() -> underTest.executeTracedJob(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageStartingWith("No stack patcher implementation found for type LOGGING_AGENT_AUTO_RESTART");
        verifyUnschedule();
    }

    private void setStackStatus(Status status) {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(status);
        stack.setStackStatus(stackStatus);
    }

    /**
     * workaround for collection injection
     */
    private void setStackPatchServices() {
        try {
            Field existingStackPatchServicesField = ExistingStackPatcherJob.class.getDeclaredField("existingStackPatchServices");
            ReflectionUtils.makeAccessible(existingStackPatchServicesField);
            Set<ExistingStackPatchService> existingStackPatchServices = new HashSet<>();
            existingStackPatchServices.add(existingStackPatchService);
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
