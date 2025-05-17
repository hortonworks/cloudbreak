package com.sequenceiq.cloudbreak.job.instancechecker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.conf.InstanceCheckerConfig;
import com.sequenceiq.cloudbreak.metering.config.MeteringConfig;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class InstanceCheckerJobInitializerTest {

    @Mock
    private StackService stackService;

    @Mock
    private InstanceCheckerJobService instanceCheckerJobService;

    @Mock
    private InstanceCheckerConfig instanceCheckerConfig;

    @Mock
    private MeteringConfig meteringConfig;

    @InjectMocks
    private InstanceCheckerJobInitializer underTest;

    static Stream<Arguments> enabledArguments() {
        return Stream.of(
                Arguments.of(true, true, true),
                Arguments.of(true, true, false),
                Arguments.of(true, false, true),
                Arguments.of(true, false, false),
                Arguments.of(false, true, true)
        );
    }

    @MethodSource("enabledArguments")
    @ParameterizedTest
    void testInitJobsWithAliveStacks(boolean instanceCheckerConfigEnabled, boolean meteringEnabled, boolean meteringInstanceCheckerEnabled) {
        when(instanceCheckerConfig.isEnabled()).thenReturn(instanceCheckerConfigEnabled);
        lenient().when(meteringConfig.isEnabled()).thenReturn(meteringEnabled);
        lenient().when(meteringConfig.isInstanceCheckerEnabled()).thenReturn(meteringInstanceCheckerEnabled);
        when(stackService.getAllWhereStatusNotIn(anySet())).thenReturn(List.of());
        JobResource jobResource1 = mock(JobResource.class);
        JobResource jobResource2 = mock(JobResource.class);
        when(instanceCheckerConfig.isEnabled()).thenReturn(true);
        when(stackService.getAllWhereStatusNotIn(anySet())).thenReturn(List.of(jobResource1, jobResource2));

        underTest.initJobs();

        verify(instanceCheckerJobService, times(2)).schedule(any(InstanceCheckerJobAdapter.class));
    }

    @MethodSource("enabledArguments")
    @ParameterizedTest
    void testInitJobsWithoutAliveStacks(boolean instanceCheckerConfigEnabled, boolean meteringEnabled, boolean meteringInstanceCheckerEnabled) {
        when(instanceCheckerConfig.isEnabled()).thenReturn(instanceCheckerConfigEnabled);
        lenient().when(meteringConfig.isEnabled()).thenReturn(meteringEnabled);
        lenient().when(meteringConfig.isInstanceCheckerEnabled()).thenReturn(meteringInstanceCheckerEnabled);
        when(stackService.getAllWhereStatusNotIn(anySet())).thenReturn(List.of());

        underTest.initJobs();

        verifyNoInteractions(instanceCheckerJobService);
    }

    static Stream<Arguments> disabledArguments() {
        return Stream.of(
                Arguments.of(false, false, false),
                Arguments.of(false, true, false),
                Arguments.of(false, false, true)
        );
    }

    @MethodSource("disabledArguments")
    @ParameterizedTest
    void testInitJobsWhenInstanceCheckerDisabled(boolean instanceCheckerConfigEnabled, boolean meteringEnabled, boolean meteringInstanceCheckerEnabled) {
        when(instanceCheckerConfig.isEnabled()).thenReturn(instanceCheckerConfigEnabled);
        when(meteringConfig.isEnabled()).thenReturn(meteringEnabled);
        lenient().when(meteringConfig.isInstanceCheckerEnabled()).thenReturn(meteringInstanceCheckerEnabled);

        underTest.initJobs();

        verifyNoInteractions(instanceCheckerJobService);
    }
}