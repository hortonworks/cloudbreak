package com.sequenceiq.cloudbreak.job.stackpatcher;

import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.LOGGING_AGENT_AUTO_RESTART;
import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.METERING_AZURE_METADATA;
import static com.sequenceiq.cloudbreak.domain.stack.StackPatchType.UNBOUND_RESTART;
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
        when(config.getPatchConfigs()).thenReturn(Map.of(UNBOUND_RESTART, disabledConfig));

        underTest.initJobs();

        verifyNoInteractions(jobService);
    }

    @Test
    void multipleEnabled() {
        when(config.getPatchConfigs()).thenReturn(Map.of(
                UNBOUND_RESTART, getConfig(true),
                LOGGING_AGENT_AUTO_RESTART, getConfig(true),
                METERING_AZURE_METADATA, getConfig(false)));

        underTest.initJobs();

        verify(jobService, times(4)).schedule(captor.capture());
        Assertions.assertThat(captor.getAllValues())
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(UNBOUND_RESTART))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(LOGGING_AGENT_AUTO_RESTART))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_2) && a.getStackPatchType().equals(UNBOUND_RESTART))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_2) && a.getStackPatchType().equals(LOGGING_AGENT_AUTO_RESTART));
    }

    @Test
    void alreadyScheduledShouldBeRescheduled() {
        when(config.getPatchConfigs()).thenReturn(Map.of(UNBOUND_RESTART, getConfig(true)));
        doReturn(List.of(createStackPatch(stack1, UNBOUND_RESTART))).when(stackPatchService).findAllByTypeForStackIds(eq(UNBOUND_RESTART), any());

        underTest.initJobs();

        verify(stackPatchService).findAllByTypeForStackIds(UNBOUND_RESTART, Set.of(Long.valueOf(ID_1), Long.valueOf(ID_2)));
        verify(jobService, times(2)).schedule(captor.capture());
        Assertions.assertThat(captor.getAllValues())
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(UNBOUND_RESTART))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_2) && a.getStackPatchType().equals(UNBOUND_RESTART));
        verify(stackPatchService).getOrCreate(Long.valueOf(ID_1), UNBOUND_RESTART);
        verify(stackPatchService).getOrCreate(Long.valueOf(ID_2), UNBOUND_RESTART);
    }

    @Test
    void alreadyFixedShouldNotBeRescheduled() {
        when(config.getPatchConfigs()).thenReturn(Map.of(UNBOUND_RESTART, getConfig(true)));
        StackPatch stackPatch = createStackPatch(stack1, UNBOUND_RESTART);
        stackPatch.setStatus(StackPatchStatus.FIXED);
        doReturn(List.of(stackPatch)).when(stackPatchService).findAllByTypeForStackIds(eq(UNBOUND_RESTART), any());

        underTest.initJobs();

        verify(jobService, times(1)).schedule(captor.capture());
        Assertions.assertThat(captor.getAllValues())
                .noneMatch(a -> a.getJobResource().getLocalId().equals(ID_1) && a.getStackPatchType().equals(UNBOUND_RESTART))
                .anyMatch(a -> a.getJobResource().getLocalId().equals(ID_2) && a.getStackPatchType().equals(UNBOUND_RESTART));
        verify(stackPatchService, never()).getOrCreate(Long.valueOf(ID_1), UNBOUND_RESTART);
        verify(stackPatchService).getOrCreate(Long.valueOf(ID_2), UNBOUND_RESTART);
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
