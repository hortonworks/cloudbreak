package com.sequenceiq.cloudbreak.job.stackpatcher;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.TEST_PATCH_1;
import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.TEST_PATCH_2;
import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.TEST_PATCH_3;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatch;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchStatus;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.ExistingStackPatcherConfig;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.StackPatchTypeConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackpatch.StackPatchService;

@ExtendWith(MockitoExtension.class)
class ExistingStackPatcherJobInitializerTest {

    private static final String ID_1 = "1";

    private static final String ID_2 = "2";

    private static final LocalDateTime NOW = LocalDateTime.of(2019, 1, 1, 0, 0, 0);

    private static final long FIRST_RUN_OFFSET = 1L;

    private static final int CHUNK_SIZE = 2;

    private static final int MAX_INITIAL_START_DELAY_IN_HOURS = 3;

    @InjectMocks
    private ExistingStackPatcherJobInitializer underTest;

    @Mock
    private ExistingStackPatcherConfig config;

    @Mock
    private ExistingStackPatcherJobService jobService;

    @Mock
    private StackService stackService;

    @Mock
    private StackPatchService stackPatchService;

    @Mock
    private JobResource stack1;

    @Mock
    private JobResource stack2;

    @Captor
    private ArgumentCaptor<ExistingStackPatcherJobAdapter> captor;

    @BeforeEach
    void setUp() {
        lenient().when(stack1.getLocalId()).thenReturn(ID_1);
        lenient().when(stack2.getLocalId()).thenReturn(ID_2);
        lenient().when(stackService.getAllWhereStatusNotIn(Status.getUnschedulableStatuses()))
                .thenReturn(List.of(stack1, stack2));

        lenient().when(stackPatchService.findAllByTypeForStackIds(any(), any())).thenReturn(List.of());
        lenient().when(config.getInitializationChunkSize()).thenReturn(CHUNK_SIZE);
        lenient().when(config.getMaxInitialStartDelayInHours()).thenReturn(MAX_INITIAL_START_DELAY_IN_HOURS);

        underTest.nowSupplier = () -> NOW;
        underTest.randomLongProvider = limit -> FIRST_RUN_OFFSET;
    }

    @Test
    void emptyEnabled() {
        StackPatchTypeConfig disabledConfig = getConfig(false);
        when(config.getPatchConfigs()).thenReturn(Map.of(TEST_PATCH_2, disabledConfig));

        underTest.initJobs();

        verifyNoInteractions(jobService);
    }

    @Test
    void multipleEnabled() {
        when(config.getPatchConfigs()).thenReturn(Map.of(
                TEST_PATCH_1, getConfig(true),
                TEST_PATCH_2, getConfig(true),
                TEST_PATCH_3, getConfig(false)));

        underTest.initJobs();

        verify(jobService, times(4)).schedule(captor.capture(), any());
        Assertions.assertThat(captor.getAllValues())
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(TEST_PATCH_2))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(TEST_PATCH_1))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_2) && a.getStackPatchType().equals(TEST_PATCH_2))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_2) && a.getStackPatchType().equals(TEST_PATCH_1));
    }

    @Test
    void alreadyScheduledShouldBeRescheduled() {
        when(config.getPatchConfigs()).thenReturn(Map.of(TEST_PATCH_2, getConfig(true)));
        doReturn(List.of(createStackPatch(stack1, TEST_PATCH_2))).when(stackPatchService).findAllByTypeForStackIds(eq(TEST_PATCH_2), any());

        underTest.initJobs();

        verify(stackPatchService).findAllByTypeForStackIds(TEST_PATCH_2, Set.of(Long.valueOf(ID_1), Long.valueOf(ID_2)));
        verify(jobService, times(CHUNK_SIZE)).schedule(captor.capture(), any());
        Assertions.assertThat(captor.getAllValues())
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(TEST_PATCH_2))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_2) && a.getStackPatchType().equals(TEST_PATCH_2));
        verify(stackPatchService).getOrCreate(Long.valueOf(ID_1), TEST_PATCH_2);
        verify(stackPatchService).getOrCreate(Long.valueOf(ID_2), TEST_PATCH_2);
    }

    @Test
    void alreadyFixedShouldNotBeRescheduled() {
        when(config.getPatchConfigs()).thenReturn(Map.of(TEST_PATCH_2, getConfig(true)));
        StackPatch stackPatch = createStackPatch(stack1, TEST_PATCH_2);
        stackPatch.setStatus(StackPatchStatus.FIXED);
        doReturn(List.of(stackPatch)).when(stackPatchService).findAllByTypeForStackIds(eq(TEST_PATCH_2), any());

        underTest.initJobs();

        verify(jobService, times(1)).schedule(captor.capture(), any());
        Assertions.assertThat(captor.getAllValues())
                .noneMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(TEST_PATCH_2))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_2) && a.getStackPatchType().equals(TEST_PATCH_2));
        verify(stackPatchService, never()).getOrCreate(Long.valueOf(ID_1), TEST_PATCH_2);
        verify(stackPatchService).getOrCreate(Long.valueOf(ID_2), TEST_PATCH_2);
    }

    @Test
    void queryStacksInChunks() {
        JobResource stack3 = mock();
        String id3 = "3";
        when(stack3.getLocalId()).thenReturn(id3);
        when(stackService.getAllWhereStatusNotIn(Status.getUnschedulableStatuses()))
                .thenReturn(List.of(stack1, stack2, stack3));
        when(config.getPatchConfigs()).thenReturn(Map.of(
                TEST_PATCH_1, getConfig(true),
                TEST_PATCH_2, getConfig(true)));

        underTest.initJobs();

        verify(stackPatchService).findAllByTypeForStackIds(TEST_PATCH_1, Set.of(Long.valueOf(ID_1), Long.valueOf(ID_2)));
        verify(stackPatchService).findAllByTypeForStackIds(TEST_PATCH_2, Set.of(Long.valueOf(ID_1), Long.valueOf(ID_2)));
        verify(stackPatchService).findAllByTypeForStackIds(TEST_PATCH_2, Set.of(3L));
        verify(stackPatchService).findAllByTypeForStackIds(TEST_PATCH_2, Set.of(3L));
        verify(jobService, times(6)).schedule(captor.capture(), any());
        Assertions.assertThat(captor.getAllValues())
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(TEST_PATCH_2))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(TEST_PATCH_1))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_2) && a.getStackPatchType().equals(TEST_PATCH_2))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_2) && a.getStackPatchType().equals(TEST_PATCH_1))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(id3) && a.getStackPatchType().equals(TEST_PATCH_1))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(id3) && a.getStackPatchType().equals(TEST_PATCH_1));
    }

    @Test
    void scheduleMultipleStackPatchesForStackWhenOneFails() {
        when(stackPatchService.getOrCreate(Long.valueOf(ID_1), TEST_PATCH_1)).thenThrow(new RuntimeException());
        when(stackPatchService.getOrCreate(Long.valueOf(ID_1), TEST_PATCH_2)).thenReturn(mock());
        when(stackService.getAllWhereStatusNotIn(Status.getUnschedulableStatuses()))
                .thenReturn(List.of(stack1));
        when(config.getPatchConfigs()).thenReturn(Map.of(
                TEST_PATCH_1, getConfig(true),
                TEST_PATCH_2, getConfig(true)));

        underTest.initJobs();

        verify(jobService).schedule(captor.capture(), any());
        Assertions.assertThat(captor.getAllValues())
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(TEST_PATCH_2));
    }

    @Test
    void scheduleMultipleStackPatchesForStackWithTimeOffset() {
        // max offset is 3 hours and there are 2 patches, so the offset will be 90 minutes
        int minutesBetweenFirstStarts = 90;

        when(stackService.getAllWhereStatusNotIn(Status.getUnschedulableStatuses()))
                .thenReturn(List.of(stack1));
        when(config.getPatchConfigs()).thenReturn(Map.of(
                TEST_PATCH_1, getConfig(true),
                TEST_PATCH_2, getConfig(true)));

        underTest.initJobs();

        verify(jobService).schedule(captor.capture(), eq(NOW.plusMinutes(FIRST_RUN_OFFSET)));
        verify(jobService).schedule(captor.capture(), eq(NOW.plusMinutes(FIRST_RUN_OFFSET).plusMinutes(minutesBetweenFirstStarts)));
        Assertions.assertThat(captor.getAllValues())
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(TEST_PATCH_2))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(TEST_PATCH_1));
    }

    @Test
    void scheduleMultipleStackPatchesForStackWithTimeOffsetOverflow() {
        // max offset is 3 hours and there are 2 patches, so the offset will be 90 minutes
        int minutesBetweenFirstStarts = 90;
        long firstRunOffset = 120L;
        underTest.randomLongProvider = limit -> firstRunOffset;
        // first run will have 120min offset, so the second would have 210min, but it is more than the max 180min (3h), so only use the remainder as offset
        long overflow = (firstRunOffset + minutesBetweenFirstStarts) % TimeUnit.HOURS.toMinutes(MAX_INITIAL_START_DELAY_IN_HOURS);

        when(stackService.getAllWhereStatusNotIn(Status.getUnschedulableStatuses()))
                .thenReturn(List.of(stack1));
        when(config.getPatchConfigs()).thenReturn(Map.of(
                TEST_PATCH_1, getConfig(true),
                TEST_PATCH_2, getConfig(true)));

        underTest.initJobs();

        verify(jobService).schedule(captor.capture(), eq(NOW.plusMinutes(firstRunOffset)));
        verify(jobService).schedule(captor.capture(), eq(NOW.plusMinutes(overflow)));
        Assertions.assertThat(captor.getAllValues())
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(TEST_PATCH_2))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(TEST_PATCH_1));
    }

    private StackPatchTypeConfig getConfig(boolean enabled) {
        StackPatchTypeConfig disabledConfig = new StackPatchTypeConfig();
        disabledConfig.setEnabled(enabled);
        return disabledConfig;
    }

    private StackPatch createStackPatch(JobResource jobResource, StackPatchType stackPatchType) {
        Stack stack = new Stack();
        stack.setId(Long.valueOf(jobResource.getLocalId()));
        return new StackPatch(stack, stackPatchType);
    }

}
