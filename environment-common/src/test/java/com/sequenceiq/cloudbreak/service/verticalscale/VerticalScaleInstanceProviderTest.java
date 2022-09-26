package com.sequenceiq.cloudbreak.service.verticalscale;


import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmTypeWithMeta;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@ExtendWith(MockitoExtension.class)
public class VerticalScaleInstanceProviderTest {

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

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validInstanceTypeForVerticalScaling(current, requested);
        });

        assertEquals("The current instancetype m3.xlarge has more Memory then the requested m2.xlarge.",
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

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validInstanceTypeForVerticalScaling(current, requested);
        });

        assertEquals("The current instancetype m3.xlarge has more CPU then the requested m2.xlarge.",
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