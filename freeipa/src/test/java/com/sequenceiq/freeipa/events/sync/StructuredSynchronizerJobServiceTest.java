package com.sequenceiq.freeipa.events.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.quartz.SimpleTrigger.REPEAT_INDEFINITELY;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.DateBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.quartz.configuration.scheduler.TransactionalScheduler;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.freeipa.repository.StackRepository;

@ExtendWith(MockitoExtension.class)
class StructuredSynchronizerJobServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StructuredSynchronizerConfig structuredSynchronizerConfig;

    @Mock
    private TransactionalScheduler scheduler;

    @Mock
    private Clock clock;

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private StackRepository stackRepository;

    @InjectMocks
    private StructuredSynchronizerJobService underTest;

    @Captor
    private ArgumentCaptor<JobDetail> jobDetailCaptor;

    @Captor
    private ArgumentCaptor<SimpleTrigger> triggerCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(structuredSynchronizerConfig.isStructuredSyncEnabled()).thenReturn(true);
        lenient().when(structuredSynchronizerConfig.getIntervalInHours()).thenReturn(1);
    }

    @Test
    void testGetJobGroup() {
        assertEquals("structured-synchronizer-jobs", underTest.getJobGroup());
    }

    @Test
    void testGetScheduler() {
        assertEquals(scheduler, underTest.getScheduler());
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest
    void testScheduleWhenStructuredSyncDisabled(boolean withDelay) {
        StructuredSynchronizerJobAdapter jobResourceAdapter = mock(StructuredSynchronizerJobAdapter.class);
        JobResource jobResource = mock(JobResource.class);
        when(structuredSynchronizerConfig.isStructuredSyncEnabled()).thenReturn(false);
        when(jobResourceAdapter.getJobResource()).thenReturn(jobResource);
        when(jobResource.getLocalId()).thenReturn(STACK_ID.toString());

        underTest.schedule(jobResourceAdapter, withDelay);

        verifyNoInteractions(scheduler);
    }

    private static Stream<Arguments> testScheduleArguments() {
        return Stream.of(
                // withDelay, jobAlreadyExists
                Arguments.of(false, false),
                Arguments.of(false, true),
                Arguments.of(true, false),
                Arguments.of(true, true)
        );
    }

    @MethodSource("testScheduleArguments")
    @ParameterizedTest
    void testSchedule(boolean withDelay, boolean jobExistsWithKey) throws SchedulerException, TransactionService.TransactionExecutionException {
        StructuredSynchronizerJobAdapter jobResourceAdapter = mock(StructuredSynchronizerJobAdapter.class);
        JobResource jobResource = mock(JobResource.class);
        JobDataMap jobDataMap = new JobDataMap(Map.of("key1", "value1", "key2", "value2"));
        Date date = mock(Date.class);
        when(jobResourceAdapter.getJobResource()).thenReturn(jobResource);
        when(jobResourceAdapter.getJobClassForResource()).thenCallRealMethod();
        when(jobResourceAdapter.toJobDataMap()).thenReturn(jobDataMap);
        when(jobResource.getLocalId()).thenReturn(STACK_ID.toString());
        when(scheduler.getJobDetail(JobKey.jobKey(STACK_ID.toString(), "structured-synchronizer-jobs")))
                .thenReturn(jobExistsWithKey ? mock(JobDetail.class) : null);
        lenient().when(clock.getDateForDelayedStart(any())).thenReturn(date);

        underTest.schedule(jobResourceAdapter, withDelay);

        if (jobExistsWithKey) {
            verify(scheduler).deleteJob(JobKey.jobKey(STACK_ID.toString(), "structured-synchronizer-jobs"));
        }
        verify(scheduler).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());
        JobDetail jobDetail = jobDetailCaptor.getValue();
        assertEquals(StructuredSynchronizerJob.class, jobDetail.getJobClass());
        assertEquals(JobKey.jobKey(STACK_ID.toString(), "structured-synchronizer-jobs"), jobDetail.getKey());
        assertEquals("Creating Freeipa Structured Synchronization Event", jobDetail.getDescription());
        assertThat(jobDetail.getJobDataMap()).containsExactlyInAnyOrderEntriesOf(jobDataMap);
        assertTrue(jobDetail.isDurable());
        SimpleTrigger trigger = triggerCaptor.getValue();
        assertEquals(jobDetail.getKey(), trigger.getJobKey());
        assertThat(trigger.getJobDataMap()).containsAllEntriesOf(jobDataMap);
        assertEquals(TriggerKey.triggerKey(STACK_ID.toString(), "structured-synchronizer-triggers"), trigger.getKey());
        assertEquals("Freeipa Structured Synchronization Event Trigger", trigger.getDescription());
        if (withDelay) {
            assertEquals(date, trigger.getStartTime());
        }
        assertEquals(DateBuilder.MILLISECONDS_IN_HOUR, trigger.getRepeatInterval());
        assertEquals(REPEAT_INDEFINITELY, trigger.getRepeatCount());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT, trigger.getMisfireInstruction());
    }

    @MethodSource("testScheduleArguments")
    @ParameterizedTest
    void testScheduleUsingReflection(boolean withDelay, boolean jobExistsWithKey) throws SchedulerException, TransactionService.TransactionExecutionException {
        JobResource jobResource = mock(JobResource.class);
        Date date = mock(Date.class);
        Map<String, String> expectedJobDataMap = Map.of(
                "localId", STACK_ID.toString(),
                "remoteResourceCrn", "remoteResourceId",
                "provider", "AWS"
        );
        when(jobResource.getLocalId()).thenReturn(STACK_ID.toString());
        when(jobResource.getRemoteResourceId()).thenReturn("remoteResourceId");
        when(jobResource.getProvider()).thenReturn(Optional.of("AWS"));
        when(scheduler.getJobDetail(JobKey.jobKey(STACK_ID.toString(), "structured-synchronizer-jobs")))
                .thenReturn(jobExistsWithKey ? mock(JobDetail.class) : null);
        lenient().when(clock.getDateForDelayedStart(any())).thenReturn(date);
        when(applicationContext.getBean(StackRepository.class)).thenReturn(stackRepository);
        when(stackRepository.getJobResource(STACK_ID)).thenReturn(Optional.of(jobResource));

        underTest.schedule(STACK_ID, StructuredSynchronizerJobAdapter.class, withDelay);

        if (jobExistsWithKey) {
            verify(scheduler).deleteJob(JobKey.jobKey(STACK_ID.toString(), "structured-synchronizer-jobs"));
        }
        verify(scheduler).scheduleJob(jobDetailCaptor.capture(), triggerCaptor.capture());
        JobDetail jobDetail = jobDetailCaptor.getValue();
        assertEquals(StructuredSynchronizerJob.class, jobDetail.getJobClass());
        assertEquals(JobKey.jobKey(STACK_ID.toString(), "structured-synchronizer-jobs"), jobDetail.getKey());
        assertEquals("Creating Freeipa Structured Synchronization Event", jobDetail.getDescription());
        assertThat(jobDetail.getJobDataMap()).containsExactlyInAnyOrderEntriesOf(expectedJobDataMap);
        assertTrue(jobDetail.isDurable());
        SimpleTrigger trigger = triggerCaptor.getValue();
        assertEquals(jobDetail.getKey(), trigger.getJobKey());
        assertThat(trigger.getJobDataMap()).containsExactlyInAnyOrderEntriesOf(expectedJobDataMap);
        assertEquals(TriggerKey.triggerKey(STACK_ID.toString(), "structured-synchronizer-triggers"), trigger.getKey());
        assertEquals("Freeipa Structured Synchronization Event Trigger", trigger.getDescription());
        if (withDelay) {
            assertEquals(date, trigger.getStartTime());
        }
        assertEquals(DateBuilder.MILLISECONDS_IN_HOUR, trigger.getRepeatInterval());
        assertEquals(REPEAT_INDEFINITELY, trigger.getRepeatCount());
        assertEquals(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT, trigger.getMisfireInstruction());
    }

    @Test
    void testScheduleDoesNotThrow() throws SchedulerException, TransactionService.TransactionExecutionException {
        StructuredSynchronizerJobAdapter jobResourceAdapter = mock(StructuredSynchronizerJobAdapter.class);
        JobResource jobResource = mock(JobResource.class);
        JobDataMap jobDataMap = new JobDataMap(Map.of("key1", "value1", "key2", "value2"));
        when(jobResourceAdapter.getJobResource()).thenReturn(jobResource);
        when(jobResourceAdapter.getJobClassForResource()).thenCallRealMethod();
        when(jobResourceAdapter.toJobDataMap()).thenReturn(jobDataMap);
        when(jobResource.getLocalId()).thenReturn(STACK_ID.toString());
        when(scheduler.getJobDetail(JobKey.jobKey(STACK_ID.toString(), "structured-synchronizer-jobs"))).thenReturn(null);
        doThrow(RuntimeException.class).when(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));

        assertDoesNotThrow(() -> underTest.schedule(jobResourceAdapter, false));
    }

    @Test
    void testScheduleUsingReflectionDoesNotThrow() {
        when(applicationContext.getBean(StackRepository.class)).thenThrow(NoSuchBeanDefinitionException.class);

        assertDoesNotThrow(() -> underTest.schedule(STACK_ID, StructuredSynchronizerJobAdapter.class, false));
    }

    @Test
    void testUnscheduleDoesNotThrow() throws TransactionService.TransactionExecutionException {
        doThrow(RuntimeException.class).when(scheduler).deleteJob(JobKey.jobKey(STACK_ID.toString(), "structured-synchronizer-jobs"));

        assertDoesNotThrow(() -> underTest.unschedule(STACK_ID.toString()));
    }

}
