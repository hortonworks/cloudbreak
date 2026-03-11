package com.sequenceiq.cloudbreak.rotation.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.config.PeriodicRotationProperties;
import com.sequenceiq.cloudbreak.rotation.repository.SecretRotationHistoryRepository;
import com.sequenceiq.cloudbreak.rotation.service.history.SecretRotationHistoryService;
import com.sequenceiq.cloudbreak.rotation.service.periodic.PeriodicSecretRotationService;

@ExtendWith(MockitoExtension.class)
class PeriodicSecretRotationJobTest {

    private static final String RESOURCE_CRN = "crn:cdp:freeipa:us-west-1:12345:freeipa:abcd";

    @Mock
    private PeriodicRotationProperties periodicRotationProperties;

    @Mock
    private SecretRotationHistoryService secretRotationHistoryService;

    @Mock
    private SecretRotationHistoryRepository secretRotationHistoryRepository;

    @Mock
    private PeriodicSecretRotationService rotationService;

    @Mock
    private JobExecutionContext jobExecutionContext;

    @InjectMocks
    private PeriodicSecretRotationJob job;

    @Captor
    private ArgumentCaptor<Instant> resourceCreationDateCaptor;

    @BeforeEach
    void setup() {
        lenient().when(periodicRotationProperties.isEnabled()).thenReturn(true);
        lenient().when(rotationService.enabledSecretTypes()).thenReturn(List.of(TestSecretType.TEST));
        lenient().when(periodicRotationProperties.resolveIntervalsToSecretTypes(List.of(TestSecretType.TEST)))
                .thenReturn(Map.of(TestSecretType.TEST, Duration.ofDays(30)));
        lenient().when(rotationService.isSchedulable(RESOURCE_CRN)).thenReturn(true);
        lenient().when(rotationService.listRotatableSecretNames(RESOURCE_CRN)).thenReturn(List.of("TEST", "UNKNOWN"));
        lenient().when(rotationService.getResourceCreationDate(eq(RESOURCE_CRN))).thenReturn(Instant.now());

        JobDataMap dataMap = new JobDataMap(Map.of(
                "localId", "test-id",
                "remoteResourceCrn", RESOURCE_CRN));
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(dataMap);
        // QuartzJobBean expects a non-null scheduler with a context
        Scheduler scheduler = org.mockito.Mockito.mock(Scheduler.class);
        when(jobExecutionContext.getScheduler()).thenReturn(scheduler);
        try {
            when(scheduler.getContext()).thenReturn(new SchedulerContext());
        } catch (org.quartz.SchedulerException e) {
            throw new RuntimeException(e);
        }
        // Provide JobDetail for MDC logging
        JobDetail jobDetail = org.mockito.Mockito.mock(JobDetail.class);
        when(jobExecutionContext.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobClass()).thenReturn((Class) PeriodicSecretRotationJob.class);
        when(jobDetail.getKey()).thenReturn(new JobKey("test-job", "test-group"));
    }

    @Test
    void rotatesWhenSecretIsDue() throws Exception {
        when(secretRotationHistoryService.checkIfRotationDue(eq(RESOURCE_CRN), eq(TestSecretType.TEST), eq(Duration.ofDays(30)), any(Instant.class)))
                .thenReturn(true);

        job.execute(jobExecutionContext);

        verify(rotationService).triggerRotation(eq(RESOURCE_CRN), eq(List.of("TEST")));
    }

    @Test
    void doesNotRotateWhenSecretNotDue() throws Exception {
        when(secretRotationHistoryService.checkIfRotationDue(eq(RESOURCE_CRN), eq(TestSecretType.TEST), eq(Duration.ofDays(30)), any(Instant.class)))
                .thenReturn(false);

        job.execute(jobExecutionContext);

        verify(rotationService, never()).triggerRotation(any(), any());
    }

    @Test
    void skipsWhenPeriodicDisabledByService() throws Exception {
        when(periodicRotationProperties.isEnabled()).thenReturn(false);

        job.execute(jobExecutionContext);

        verify(rotationService, never()).triggerRotation(any(), any());
    }

    @Test
    void skipsWhenNoIntervalsResolved() throws Exception {
        when(periodicRotationProperties.resolveIntervalsToSecretTypes(List.of(TestSecretType.TEST)))
                .thenReturn(Map.of());

        job.execute(jobExecutionContext);

        verify(rotationService, never()).triggerRotation(any(), any());
    }

    @Test
    void skipsWhenNoResourceCrnProvided() {
        JobDataMap dataMap = new JobDataMap(Map.of());
        when(jobExecutionContext.getMergedJobDataMap()).thenReturn(dataMap);

        assertThatThrownBy(() -> job.execute(jobExecutionContext))
                .isInstanceOf(JobExecutionException.class);

        verify(rotationService, never()).triggerRotation(any(), any());
    }

    @Test
    void skipsWhenResourceNotFound() throws Exception {
        when(rotationService.isSchedulable(RESOURCE_CRN)).thenReturn(false);

        job.execute(jobExecutionContext);

        verify(rotationService, never()).triggerRotation(any(), any());
    }

    @Test
    void skipsWhenNotSchedulable() throws Exception {
        when(rotationService.isSchedulable(RESOURCE_CRN)).thenReturn(false);

        job.execute(jobExecutionContext);

        verify(rotationService, never()).triggerRotation(any(), any());
    }

    @Test
    void filtersUnknownSecretTypesOnlyKnownCheckedAndTriggered() throws Exception {
        when(rotationService.listRotatableSecretNames(RESOURCE_CRN)).thenReturn(List.of("TEST", "UNKNOWN"));
        when(secretRotationHistoryService.checkIfRotationDue(eq(RESOURCE_CRN), eq(TestSecretType.TEST), eq(Duration.ofDays(30)), any(Instant.class)))
                .thenReturn(true);

        job.execute(jobExecutionContext);

        // Only TEST should be triggered, not UNKNOWN
        verify(rotationService, times(1)).triggerRotation(eq(RESOURCE_CRN), eq(List.of("TEST")));
    }

    @Test
    void getResourceCreationDateIsPassedToCheckIfRotationDue() throws Exception {
        Instant creationDate = Instant.now().minus(Duration.ofDays(60));
        when(rotationService.getResourceCreationDate(eq(RESOURCE_CRN))).thenReturn(creationDate);
        when(secretRotationHistoryService.checkIfRotationDue(eq(RESOURCE_CRN), eq(TestSecretType.TEST),
            eq(Duration.ofDays(30)), resourceCreationDateCaptor.capture()))
                .thenReturn(true);

        job.execute(jobExecutionContext);

        verify(rotationService).getResourceCreationDate(eq(RESOURCE_CRN));
        assertThat(resourceCreationDateCaptor.getValue()).isEqualTo(creationDate);
    }

    @Test
    void rotatesWhenNoHistoryAndCreationDateMakesNextDuePast() throws Exception {
        SecretRotationHistoryService realHistoryService = new SecretRotationHistoryService();
        ReflectionTestUtils.setField(realHistoryService, "repository", secretRotationHistoryRepository);
        ReflectionTestUtils.setField(job, "secretRotationHistoryService", realHistoryService);

        when(secretRotationHistoryRepository.findByResourceCrnAndSecretType(eq(RESOURCE_CRN), eq(TestSecretType.TEST)))
                .thenReturn(Optional.empty());
        when(rotationService.getResourceCreationDate(eq(RESOURCE_CRN)))
                .thenReturn(Instant.now().minus(Duration.ofDays(20)));

        job.execute(jobExecutionContext);

        verify(rotationService).triggerRotation(eq(RESOURCE_CRN), eq(List.of("TEST")));
    }

    @Test
    void doesNotRotateWhenNoHistoryAndCreationDateMakesNextDueBeyondBuffer() throws Exception {
        SecretRotationHistoryService realHistoryService = new SecretRotationHistoryService();
        ReflectionTestUtils.setField(realHistoryService, "repository", secretRotationHistoryRepository);
        ReflectionTestUtils.setField(job, "secretRotationHistoryService", realHistoryService);

        when(secretRotationHistoryRepository.findByResourceCrnAndSecretType(eq(RESOURCE_CRN), eq(TestSecretType.TEST)))
                .thenReturn(Optional.empty());
        when(rotationService.getResourceCreationDate(eq(RESOURCE_CRN)))
                .thenReturn(Instant.now().minus(Duration.ofDays(5)));

        job.execute(jobExecutionContext);

        verify(rotationService, never()).triggerRotation(any(), any());
    }
}

