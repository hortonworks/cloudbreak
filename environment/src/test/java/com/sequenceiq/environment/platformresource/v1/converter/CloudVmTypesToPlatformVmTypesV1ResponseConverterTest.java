package com.sequenceiq.environment.platformresource.v1.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.VirtualMachinesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse;

@ExtendWith(MockitoExtension.class)
class CloudVmTypesToPlatformVmTypesV1ResponseConverterTest {

    private static final String ZONE = "us-east-1a";

    @Mock
    private VmTypeToVmTypeV1ResponseConverter vmTypeToVmTypeV1ResponseConverter;

    @InjectMocks
    private CloudVmTypesToPlatformVmTypesV1ResponseConverter underTest;

    private VmType vmType(String name) {
        return VmType.vmTypeWithMeta(name, VmTypeMeta.VmTypeMetaBuilder.builder().create(), true);
    }

    private VmTypeResponse vmTypeResponse(String value) {
        VmTypeResponse r = new VmTypeResponse();
        r.setValue(value);
        return r;
    }

    @Test
    void convertPopulatesAllVmTypesInVmTypesMap() {
        VmType vm = vmType("Standard_D4s_v3");
        CloudVmTypes source = new CloudVmTypes(Map.of(ZONE, Set.of(vm)), Map.of(), Map.of());

        when(vmTypeToVmTypeV1ResponseConverter.convert(any(VmType.class))).thenReturn(vmTypeResponse("Standard_D4s_v3"));

        PlatformVmtypesResponse result = underTest.convert(source);

        VirtualMachinesResponse zoneResponse = result.getVmTypes().get(ZONE);
        assertThat(zoneResponse).isNotNull();
        assertThat(zoneResponse.getVirtualMachines()).hasSize(1);
        assertThat(zoneResponse.getVirtualMachines().iterator().next().getValue()).isEqualTo("Standard_D4s_v3");
    }

    @Test
    void convertPopulatesDeprecatedVmTypesWithinSameZoneEntry() {
        VmType regular = vmType("Standard_D4s_v3");
        VmType deprecated = vmType("Standard_D4_v2");
        CloudVmTypes source = new CloudVmTypes(
                Map.of(ZONE, Set.of(regular, deprecated)),
                Map.of(ZONE, Set.of(deprecated)),
                Map.of()
        );

        when(vmTypeToVmTypeV1ResponseConverter.convert(any(VmType.class))).thenAnswer(inv -> {
            VmType vm = inv.getArgument(0);
            return vmTypeResponse(vm.getValue());
        });

        PlatformVmtypesResponse result = underTest.convert(source);

        assertThat(result.getVmTypes()).hasSize(1);
        VirtualMachinesResponse zoneResponse = result.getVmTypes().get(ZONE);
        assertThat(zoneResponse.getVirtualMachines()).hasSize(2);
        assertThat(zoneResponse.getDeprecatedVirtualMachines()).hasSize(1);
        assertThat(zoneResponse.getDeprecatedVirtualMachines().iterator().next().getValue()).isEqualTo("Standard_D4_v2");
    }

    @Test
    void convertSetsDefaultVmType() {
        VmType vm = vmType("Standard_D4s_v3");
        CloudVmTypes source = new CloudVmTypes(
                Map.of(ZONE, Set.of(vm)),
                Map.of(),
                Map.of(ZONE, vm)
        );

        when(vmTypeToVmTypeV1ResponseConverter.convert(any(VmType.class))).thenReturn(vmTypeResponse("Standard_D4s_v3"));

        PlatformVmtypesResponse result = underTest.convert(source);

        VirtualMachinesResponse zoneResponse = result.getVmTypes().get(ZONE);
        assertThat(zoneResponse.getDefaultVirtualMachine()).isNotNull();
        assertThat(zoneResponse.getDefaultVirtualMachine().getValue()).isEqualTo("Standard_D4s_v3");
    }

    @Test
    void convertWithNoDeprecatedLeavesDeprecatedVirtualMachinesEmpty() {
        VmType vm = vmType("Standard_D4s_v3");
        CloudVmTypes source = new CloudVmTypes(Map.of(ZONE, Set.of(vm)), Map.of(), Map.of());

        when(vmTypeToVmTypeV1ResponseConverter.convert(any(VmType.class))).thenReturn(vmTypeResponse("Standard_D4s_v3"));

        PlatformVmtypesResponse result = underTest.convert(source);

        VirtualMachinesResponse zoneResponse = result.getVmTypes().get(ZONE);
        assertThat(zoneResponse.getDeprecatedVirtualMachines()).isEmpty();
    }
}
