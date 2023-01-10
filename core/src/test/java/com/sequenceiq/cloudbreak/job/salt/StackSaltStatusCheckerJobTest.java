package com.sequenceiq.cloudbreak.job.salt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MdcContextInfoProvider;
import com.sequenceiq.cloudbreak.service.RotateSaltPasswordService;
import com.sequenceiq.cloudbreak.service.SaltPasswordStatusService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
class StackSaltStatusCheckerJobTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackSaltStatusCheckerJobService jobService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RotateSaltPasswordService rotateSaltPasswordService;

    @Mock
    private SaltPasswordStatusService saltPasswordStatusService;

    @InjectMocks
    private StackSaltStatusCheckerJob underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private JobExecutionContext context;

    @Mock
    private JobDetail jobDetail;

    private JobKey jobKey;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest.setLocalId(STACK_ID.toString());

        lenient().when(context.getJobDetail()).thenReturn(jobDetail);
        jobKey = new JobKey("key");
        lenient().when(jobDetail.getKey()).thenReturn(jobKey);

        lenient().when(stackDtoService.getByIdOpt(STACK_ID)).thenReturn(Optional.of(stackDto));
        lenient().when(stackDto.getStatus()).thenReturn(Status.AVAILABLE);
    }

    @AfterEach
    public void tearDown() {
        validateMockitoUsage();
    }

    @Test
    void getMdcContextConfigProviderWithStack() {
        StackView stackView = mock(StackView.class);
        when(stackDtoService.getStackViewByIdOpt(STACK_ID)).thenReturn(Optional.of(stackView));

        Optional<MdcContextInfoProvider> result = underTest.getMdcContextConfigProvider();

        assertThat(result).contains(stackView);
    }

    @Test
    void getMdcContextConfigProviderWithoutStack() {
        when(stackDtoService.getStackViewByIdOpt(STACK_ID)).thenReturn(Optional.empty());

        Optional<MdcContextInfoProvider> result = underTest.getMdcContextConfigProvider();

        assertThat(result).isEmpty();
    }

    @Test
    void executeTracedJobWithoutStack() throws JobExecutionException {
        when(stackDtoService.getByIdOpt(STACK_ID)).thenReturn(Optional.empty());

        underTest.executeTracedJob(context);

        verify(jobService).unschedule(jobKey);
        verifyNoInteractions(rotateSaltPasswordService);
    }

    @Test
    void executeTracedJobWithStackInUnschedulableStatus() throws JobExecutionException {
        when(stackDto.getStatus()).thenReturn(Status.DELETE_COMPLETED);

        underTest.executeTracedJob(context);

        verify(jobService).unschedule(jobKey);
        verifyNoInteractions(rotateSaltPasswordService);
    }

    @Test
    void executeTracedJobWithStackInIgnoredStatus() throws JobExecutionException {
        when(stackDto.getStatus()).thenReturn(Status.CREATE_IN_PROGRESS);

        underTest.executeTracedJob(context);

        verifyNoInteractions(jobService);
        verifyNoInteractions(rotateSaltPasswordService);
    }

    @Test
    void executeTracedJobWithStackInUnhandledStatus() throws JobExecutionException {
        when(stackDto.getStatus()).thenReturn(Status.UPDATE_REQUESTED);

        underTest.executeTracedJob(context);

        verifyNoInteractions(jobService);
        verifyNoInteractions(rotateSaltPasswordService);
    }

    @Nested
    class SyncableStack {

        @BeforeEach
        void setUp() {
            when(regionAwareInternalCrnGeneratorFactory.datahub()).thenReturn(mock(RegionAwareInternalCrnGenerator.class));
        }

        @Test
        void okStatus() throws JobExecutionException {
            when(saltPasswordStatusService.getSaltPasswordStatus(stackDto)).thenReturn(SaltPasswordStatus.OK);

            underTest.executeTracedJob(context);

            verify(saltPasswordStatusService).getSaltPasswordStatus(stackDto);
            verify(rotateSaltPasswordService).validateRotateSaltPassword(stackDto);
            verify(rotateSaltPasswordService, never()).triggerRotateSaltPassword(eq(stackDto), any());
        }

        @Test
        void failedToCheckStatus() throws JobExecutionException {
            when(saltPasswordStatusService.getSaltPasswordStatus(stackDto)).thenReturn(SaltPasswordStatus.FAILED_TO_CHECK);

            underTest.executeTracedJob(context);

            verify(saltPasswordStatusService).getSaltPasswordStatus(stackDto);
            verify(rotateSaltPasswordService).validateRotateSaltPassword(stackDto);
            verify(rotateSaltPasswordService, never()).triggerRotateSaltPassword(eq(stackDto), any());
        }

        @Test
        void expiresStatus() throws JobExecutionException {
            when(saltPasswordStatusService.getSaltPasswordStatus(stackDto)).thenReturn(SaltPasswordStatus.EXPIRES);

            underTest.executeTracedJob(context);

            verify(saltPasswordStatusService).getSaltPasswordStatus(stackDto);
            verify(rotateSaltPasswordService).validateRotateSaltPassword(stackDto);
            verify(rotateSaltPasswordService).triggerRotateSaltPassword(stackDto, RotateSaltPasswordReason.EXPIRED);
        }

        @Test
        void invalidStatus() throws JobExecutionException {
            when(saltPasswordStatusService.getSaltPasswordStatus(stackDto)).thenReturn(SaltPasswordStatus.INVALID);

            underTest.executeTracedJob(context);

            verify(saltPasswordStatusService).getSaltPasswordStatus(stackDto);
            verify(rotateSaltPasswordService).validateRotateSaltPassword(stackDto);
            verify(rotateSaltPasswordService).triggerRotateSaltPassword(stackDto, RotateSaltPasswordReason.UNAUTHORIZED);
        }

        @Test
        void error() throws JobExecutionException {
            when(stackDto.getResourceCrn()).thenReturn("crn");
            RuntimeException cause = new RuntimeException("cause");
            doThrow(cause).when(saltPasswordStatusService).getSaltPasswordStatus(stackDto);

            underTest.executeTracedJob(context);

            verify(saltPasswordStatusService).getSaltPasswordStatus(stackDto);
            verify(rotateSaltPasswordService).validateRotateSaltPassword(stackDto);
            verify(rotateSaltPasswordService).sendFailureUsageReport(stackDto.getResourceCrn(), RotateSaltPasswordReason.UNSET,
                    "Failed to get salt password status: " + cause.getMessage());
        }

    }

}
