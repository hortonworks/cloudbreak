package com.sequenceiq.cloudbreak.service.verticalscale;


import static com.sequenceiq.cloudbreak.cloud.model.VmType.vmTypeWithMeta;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.filter.MinimalHardwareFilter;

@ExtendWith(MockitoExtension.class)
public class VerticalScaleInstanceProviderTest {

    private static final String INSTANCE_TYPE_1 = "instanceType1";

    private static final String INSTANCE_TYPE_2 = "instanceType2";

    private static final String AVAILABILITY_ZONE_1 = "availabilityZone1";

    private static final String AVAILABILITY_ZONE_2 = "availabilityZone2";

    @Mock
    private MinimalHardwareFilter minimalHardwareFilter;

    @InjectMocks
    private VerticalScaleInstanceProvider underTest;

    @Test
    public void testRequestWhenWeAreRequestedSmallerMemoryInstancesShouldDropBadRequest() {
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m2.xlarge";
        Optional<VmType> current = vmTypeOptional(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
        );
        Optional<VmType> requested = vmTypeOptional(
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
            underTest.validateInstanceTypeForVerticalScaling(current, requested, null);
        });

        assertEquals("The requested instancetype m2.xlarge has less Memory than the minimum 16 GB.",
                badRequestException.getMessage());
    }

    @Test
    public void testRequestWhenWeAreRequestedSmallerCpuInstancesShouldDropBadRequest() {
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m2.xlarge";
        Optional<VmType> current = vmTypeOptional(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
        );
        Optional<VmType> requested = vmTypeOptional(
                        instanceTypeNameInRequest,
                        1,
                        0,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
        );

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(false);
        when(minimalHardwareFilter.minCpu()).thenReturn(4);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateInstanceTypeForVerticalScaling(current, requested, null);
        });

        assertEquals("The requested instancetype m2.xlarge has less Cpu than the minimum 4 core.",
                badRequestException.getMessage());
    }

    @Test
    public void testRequestWhenWeAreRequestedInstanceWithLessEphemeralShouldDropBadRequest() {
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m2.xlarge";
        Optional<VmType> current = vmTypeOptional(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
        );
        Optional<VmType> requested = vmTypeOptional(
                        instanceTypeNameInRequest,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 0, 0, 0, 0)
        );

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(true);
        when(minimalHardwareFilter.suitableAsMinimumHardwareForMemory(any())).thenReturn(true);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateInstanceTypeForVerticalScaling(current, requested, null);
        });

        assertEquals("The current instancetype m3.xlarge has more Ephemeral Disk than the requested m2.xlarge.",
                badRequestException.getMessage());
    }

    @Test
    public void testRequestWhenWeAreRequestedInstanceWithLessAutoAttachedShouldDropBadRequest() {
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m2.xlarge";
        Optional<VmType> current = vmTypeOptional(
                        instanceTypeNameInStack,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
        );
        Optional<VmType> requested = vmTypeOptional(
                        instanceTypeNameInRequest,
                        1,
                        1,
                        new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 0, 0, 0, 0),
                        new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1)
        );

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(true);
        when(minimalHardwareFilter.suitableAsMinimumHardwareForMemory(any())).thenReturn(true);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateInstanceTypeForVerticalScaling(current, requested, null);
        });

        assertEquals("The current instancetype m3.xlarge has more Auto Attached Disk than the requested m2.xlarge.",
                badRequestException.getMessage());
    }

    @Test
    public void testRequestWhenWeAreRequestedInstanceWithResourceDiskButTheInstanceHasNoAttachedDisk() {
        String instanceTypeNameInStack = "Standard_D6S_v5";
        String instanceTypeNameInRequest = "Standard_D64_v5";
        Optional<VmType> current = vmTypeOptional(
                instanceTypeNameInStack,
                1,
                1,
                new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                false,
                List.of()
        );
        Optional<VmType> requested = vmTypeOptional(
                instanceTypeNameInRequest,
                1,
                1,
                new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                true,
                List.of()
        );

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(true);
        when(minimalHardwareFilter.suitableAsMinimumHardwareForMemory(any())).thenReturn(true);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateInstanceTypeForVerticalScaling(current, requested, null);
        });

        assertEquals("Unable to resize since changing from resource disk to non-resource disk VM size and " +
                        "vice-versa is not allowed. Please refer to https://aka.ms/AAah4sj for more details.",
                badRequestException.getMessage());
    }

    @Test
    public void testRequestWhenRequestedMultiAzInstanceWithWrongAvailabilityZones() {
        String instanceTypeNameInStack = "Standard_D6S_v5";
        String instanceTypeNameInRequest = "Standard_D64_v5";
        Optional<VmType> current = vmTypeOptional(
                instanceTypeNameInStack,
                1,
                1,
                new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                true,
                List.of("2", "3")
        );
        Optional<VmType> requested = vmTypeOptional(
                instanceTypeNameInRequest,
                1,
                1,
                new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                true,
                List.of("1", "2")
        );

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(true);
        when(minimalHardwareFilter.suitableAsMinimumHardwareForMemory(any())).thenReturn(true);

        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> {
            underTest.validateInstanceTypeForVerticalScaling(current, requested, Set.of("2", "3"));
        });

        assertEquals("Stack is MultiAz enabled but requested instance type is not supported in existing " +
                        "Availability Zones for Instance Group. Supported Availability Zones for Instance type Standard_D64_v5 : 1,2. " +
                        "Existing Availability Zones for Instance Group : 2,3", badRequestException.getMessage());
    }

    @Test
    public void testRequestWhenRequestedInstanceIsValid() {
        String instanceTypeNameInStack = "Standard_D6S_v5";
        String instanceTypeNameInRequest = "Standard_D64_v5";
        Optional<VmType> current = vmTypeOptional(
                instanceTypeNameInStack,
                1,
                1,
                new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                true,
                List.of("1", "2")
        );
        Optional<VmType> requested = vmTypeOptional(
                instanceTypeNameInRequest,
                1,
                1,
                new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                true,
                List.of("1", "2")
        );

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(true);
        when(minimalHardwareFilter.suitableAsMinimumHardwareForMemory(any())).thenReturn(true);

        assertDoesNotThrow(() -> underTest.validateInstanceTypeForVerticalScaling(current, requested, Set.of("1", "2")));
    }

    @Test
    void listInstanceTypesTestWhenNoCloudVmResponses() {
        CloudVmTypes allVmTypes = new CloudVmTypes(Map.of(), Map.of());

        CloudVmTypes result = underTest.listInstanceTypes(AVAILABILITY_ZONE_1, INSTANCE_TYPE_1, allVmTypes);

        assertThat(result).isNotNull();
        assertThat(result.getCloudVmResponses()).isEqualTo(Map.of());
        assertThat(result.getDefaultCloudVmResponses()).isEqualTo(Map.of());
    }

    @ParameterizedTest(name = "availabilityZone=\"{0}\"")
    @ValueSource(strings = {"", " "})
    @NullSource
    void listInstanceTypesTestWhenSuitableInstancesAndUnspecifiedAvailabilityZone(String availabilityZone) {
        VmType current = vmType(
                INSTANCE_TYPE_1,
                1,
                1,
                new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                false,
                List.of()
        );

        CloudVmTypes allVmTypes = new CloudVmTypes(Map.ofEntries(entry(AVAILABILITY_ZONE_1, Set.of(current))),
                Map.ofEntries(entry(AVAILABILITY_ZONE_1, current)));

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(true);
        when(minimalHardwareFilter.suitableAsMinimumHardwareForMemory(any())).thenReturn(true);

        CloudVmTypes result = underTest.listInstanceTypes(availabilityZone, INSTANCE_TYPE_1, allVmTypes);

        verifySuitableInstances(result);
    }

    private void verifySuitableInstances(CloudVmTypes result) {
        assertThat(result).isNotNull();

        Map<String, Set<VmType>> cloudVmResponses = result.getCloudVmResponses();
        assertThat(cloudVmResponses).isNotNull();
        assertThat(cloudVmResponses).hasSize(1);
        Set<VmType> vmTypes = cloudVmResponses.get(AVAILABILITY_ZONE_1);
        assertThat(vmTypes).isNotNull();
        assertThat(vmTypes).hasSize(1);
        VmType vmType = vmTypes.iterator().next();
        assertThat(vmType.getValue()).isEqualTo(INSTANCE_TYPE_1);

        Map<String, VmType> defaultCloudVmResponses = result.getDefaultCloudVmResponses();
        assertThat(defaultCloudVmResponses).isNotNull();
        assertThat(defaultCloudVmResponses).hasSize(1);
        VmType vmTypeDefault = defaultCloudVmResponses.get(AVAILABILITY_ZONE_1);
        assertThat(vmTypeDefault.getValue()).isEqualTo(INSTANCE_TYPE_1);
    }

    @Test
    void listInstanceTypesTestWhenInvalidAvailabilityZone() {
        VmType current = vmType(
                INSTANCE_TYPE_1,
                1,
                1,
                new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                false,
                List.of()
        );

        CloudVmTypes allVmTypes = new CloudVmTypes(Map.ofEntries(entry(AVAILABILITY_ZONE_1, Set.of(current))),
                Map.ofEntries(entry(AVAILABILITY_ZONE_1, current)));

        CloudVmTypes result = underTest.listInstanceTypes(AVAILABILITY_ZONE_2, INSTANCE_TYPE_1, allVmTypes);

        assertThat(result).isNotNull();
        assertThat(result.getCloudVmResponses()).isEqualTo(Map.of(AVAILABILITY_ZONE_2, Set.of()));
        assertThat(result.getDefaultCloudVmResponses()).isEqualTo(Map.of());
    }

    @Test
    void listInstanceTypesTestWhenInvalidCurrentInstanceType() {
        VmType current = vmType(
                INSTANCE_TYPE_1,
                1,
                1,
                new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                false,
                List.of()
        );

        CloudVmTypes allVmTypes = new CloudVmTypes(Map.ofEntries(entry(AVAILABILITY_ZONE_1, Set.of(current))),
                Map.ofEntries(entry(AVAILABILITY_ZONE_1, current)));

        CloudVmTypes result = underTest.listInstanceTypes(AVAILABILITY_ZONE_1, INSTANCE_TYPE_2, allVmTypes);

        assertThat(result).isNotNull();
        assertThat(result.getCloudVmResponses()).isEqualTo(Map.of(AVAILABILITY_ZONE_1, Set.of()));
        assertThat(result.getDefaultCloudVmResponses()).isEqualTo(Map.of());
    }

    @Test
    void listInstanceTypesTestWhenNoSuitableInstances() {
        VmType current = vmType(
                INSTANCE_TYPE_1,
                1,
                1,
                new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                false,
                List.of()
        );

        CloudVmTypes allVmTypes = new CloudVmTypes(Map.ofEntries(entry(AVAILABILITY_ZONE_1, Set.of(current))),
                Map.ofEntries(entry(AVAILABILITY_ZONE_1, current)));

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(false);
        when(minimalHardwareFilter.minCpu()).thenReturn(4);

        CloudVmTypes result = underTest.listInstanceTypes(AVAILABILITY_ZONE_1, INSTANCE_TYPE_1, allVmTypes);

        assertThat(result).isNotNull();
        assertThat(result.getCloudVmResponses()).isEqualTo(Map.of(AVAILABILITY_ZONE_1, Set.of()));
        assertThat(result.getDefaultCloudVmResponses()).isEqualTo(Map.of());
    }

    @Test
    void listInstanceTypesTestWhenSuitableInstancesAndGivenAvailabilityZone() {
        VmType current = vmType(
                INSTANCE_TYPE_1,
                1,
                1,
                new VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, 1, 1, 1, 1),
                new VolumeParameterConfig(VolumeParameterType.EPHEMERAL, 1, 1, 1, 1),
                false,
                List.of()
        );

        CloudVmTypes allVmTypes = new CloudVmTypes(Map.ofEntries(entry(AVAILABILITY_ZONE_1, Set.of(current))),
                Map.ofEntries(entry(AVAILABILITY_ZONE_1, current)));

        when(minimalHardwareFilter.suitableAsMinimumHardwareForCpu(any())).thenReturn(true);
        when(minimalHardwareFilter.suitableAsMinimumHardwareForMemory(any())).thenReturn(true);

        CloudVmTypes result = underTest.listInstanceTypes(AVAILABILITY_ZONE_1, INSTANCE_TYPE_1, allVmTypes);

        verifySuitableInstances(result);
    }

    private Optional<VmType> vmTypeOptional(String name, int memory, int cpu, VolumeParameterConfig autoAttached,
            VolumeParameterConfig ephemeral, boolean resourceDisk, List<String> availabilityZones) {
        return Optional.of(vmType(name, memory, cpu, autoAttached, ephemeral, resourceDisk, availabilityZones));
    }

    private Optional<VmType> vmTypeOptional(String name, int memory, int cpu, VolumeParameterConfig autoAttached, VolumeParameterConfig ephemeral) {
        return Optional.of(vmType(name, memory, cpu, autoAttached, ephemeral, false, List.of()));
    }

    private VmType vmType(String name, int memory, int cpu, VolumeParameterConfig autoAttached, VolumeParameterConfig ephemeral, boolean resourceDisk,
            List<String> availabilityZones) {
        return vmTypeWithMeta(name,
                VmTypeMeta.VmTypeMetaBuilder.builder()
                        .withAutoAttachedConfig(autoAttached)
                        .withCpuAndMemory(cpu, memory)
                        .withEphemeralConfig(ephemeral)
                        .withResourceDiskAttached(resourceDisk)
                        .withAvailabilityZones(availabilityZones)
                        .create(),
                false);
    }

}