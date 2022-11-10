package com.sequenceiq.cloudbreak.service.verticalscale;


import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmTypeWithMeta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.filter.MinimalHardwareFilter;

@ExtendWith(MockitoExtension.class)
public class VerticalScaleInstanceProviderTest {

    @Mock
    private MinimalHardwareFilter minimalHardwareFilter;

    @InjectMocks
    private VerticalScaleInstanceProvider underTest;

    @Test
    public void testRequestWhenWeAreRequestedSmallerMemoryInstancesShouldDropBadRequest() {
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m2.xlarge";
        Optional<VmType> current = vmType(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
                );
        Optional<VmType> requested = vmType(
                        instanceTypeNameInRequest,
                        0,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
                );

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(true);
        when(minimalHardwareFilter.suitableAsMinimumHardwareForMemory(any())).thenReturn(false);
        when(minimalHardwareFilter.minMemory()).thenReturn(16);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validInstanceTypeForVerticalScaling(current, requested);
        });

        assertEquals("The requested instancetype m2.xlarge has less Memory then the minimum 16 GB.",
                badRequestException.getMessage());
    }

    @Test
    public void testRequestWhenWeAreRequestedSmallerCpuInstancesShouldDropBadRequest() {
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m2.xlarge";
        Optional<VmType> current = vmType(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
                );
        Optional<VmType> requested = vmType(
                        instanceTypeNameInRequest,
                        1,
                        0,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
                );

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(false);
        when(minimalHardwareFilter.minCpu()).thenReturn(4);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validInstanceTypeForVerticalScaling(current, requested);
        });

        assertEquals("The requested instancetype m2.xlarge has less Cpu then the minimum 4 core.",
                badRequestException.getMessage());
    }

    @Test
    public void testRequestWhenWeAreRequestedInstanceWithLessEphemeralShouldDropBadRequest() {
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m2.xlarge";
        Optional<VmType> current = vmType(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
                );
        Optional<VmType> requested = vmType(
                        instanceTypeNameInRequest,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 0, 0, 0, 0)
                );

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(true);
        when(minimalHardwareFilter.suitableAsMinimumHardwareForMemory(any())).thenReturn(true);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validInstanceTypeForVerticalScaling(current, requested);
        });

        assertEquals("The current instancetype m3.xlarge has more Ephemeral Disk then the requested m2.xlarge.",
                badRequestException.getMessage());
    }

    @Test
    public void testRequestWhenWeAreRequestedInstanceWithLessAutoAttachedShouldDropBadRequest() {
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m2.xlarge";
        Optional<VmType> current = vmType(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
                );
        Optional<VmType> requested = vmType(
                        instanceTypeNameInRequest,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 0, 0, 0, 0),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
                );

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(true);
        when(minimalHardwareFilter.suitableAsMinimumHardwareForMemory(any())).thenReturn(true);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validInstanceTypeForVerticalScaling(current, requested);
        });

        assertEquals("The current instancetype m3.xlarge has more Auto Attached Disk then the requested m2.xlarge.",
                badRequestException.getMessage());
    }

    public Optional<VmType> vmType(String name, int memory, int cpu, VolumeParameterConfig autoAttached, VolumeParameterConfig ephemeral) {
        return Optional.of(vmTypeWithMeta(name,
                VmTypeMeta.VmTypeMetaBuilder.builder()
                        .withAutoAttachedConfig(autoAttached)
                        .withCpuAndMemory(cpu, memory)
                        .withEphemeralConfig(ephemeral)
                        .create(),
                false));
    }
}