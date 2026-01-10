package com.sequenceiq.cloudbreak.job.stackpatcher;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.TEST_PATCH_1;
import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.TEST_PATCH_2;
import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.TEST_PATCH_3;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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

        verify(jobService, times(4)).schedule(captor.capture());
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
        verify(jobService, times(2)).schedule(captor.capture());
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

        verify(jobService, times(1)).schedule(captor.capture());
        Assertions.assertThat(captor.getAllValues())
                .noneMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(TEST_PATCH_2))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_2) && a.getStackPatchType().equals(TEST_PATCH_2));
        verify(stackPatchService, never()).getOrCreate(Long.valueOf(ID_1), TEST_PATCH_2);
        verify(stackPatchService).getOrCreate(Long.valueOf(ID_2), TEST_PATCH_2);
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
