package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.azure.resourcemanager.compute.models.ResourceSkuCapabilities;
import com.sequenceiq.common.model.Architecture;

class AzureVmCapabilitiesTest {

    private static final String VM_TYPE = "Standard_E4pds_v5";

    @Test
    void testArchitectureArm64() {
        AzureVmCapabilities underTest = new AzureVmCapabilities(VM_TYPE, List.of(capability("CpuArchitectureType", "Arm64")));

        assertEquals(Architecture.ARM64, underTest.getArchitecture());
    }

    @Test
    void testArchitectureX64() {
        AzureVmCapabilities underTest = new AzureVmCapabilities(VM_TYPE, List.of(capability("CpuArchitectureType", "x64")));

        assertEquals(Architecture.X86_64, underTest.getArchitecture());
    }

    @Test
    void testArchitectureCapabilityAbsentFallsBackToX86() {
        AzureVmCapabilities underTest = new AzureVmCapabilities(VM_TYPE, List.of());

        assertEquals(Architecture.X86_64, underTest.getArchitecture());
    }

    @Test
    void testArchitectureUnknownValueFallsBackToX86() {
        AzureVmCapabilities underTest = new AzureVmCapabilities(VM_TYPE, List.of(capability("CpuArchitectureType", "something-else")));

        assertEquals(Architecture.X86_64, underTest.getArchitecture());
    }

    @Test
    void testArchitectureNullCapabilitiesFallsBackToX86() {
        AzureVmCapabilities underTest = new AzureVmCapabilities(VM_TYPE, null);

        assertEquals(Architecture.X86_64, underTest.getArchitecture());
    }

    private ResourceSkuCapabilities capability(String name, String value) {
        ResourceSkuCapabilities capability = mock(ResourceSkuCapabilities.class);
        when(capability.name()).thenReturn(name);
        when(capability.value()).thenReturn(value);
        return capability;
    }
}
