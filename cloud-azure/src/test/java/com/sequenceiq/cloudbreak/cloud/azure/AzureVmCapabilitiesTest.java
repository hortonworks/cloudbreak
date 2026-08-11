package com.sequenceiq.cloudbreak.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void testGen1SupportedWhenHyperVGenerationsContainsV1() {
        AzureVmCapabilities caps = new AzureVmCapabilities("Standard_D4s_v5", List.of(capability("HyperVGenerations", "V1,V2")));

        assertThat(caps.isGen1Supported()).isTrue();
        assertThat(caps.isGen2Supported()).isTrue();
    }

    @Test
    void testGen1NotSupportedWhenHyperVGenerationsIsV2Only() {
        AzureVmCapabilities caps = new AzureVmCapabilities("Standard_D4s_v6", List.of(capability("HyperVGenerations", "V2")));

        assertThat(caps.isGen1Supported()).isFalse();
        assertThat(caps.isGen2Supported()).isTrue();
    }

    @Test
    void testBothSupportedWhenHyperVGenerationsCapabilityAbsent() {
        AzureVmCapabilities caps = new AzureVmCapabilities("Standard_D4s_v5", List.of());

        assertThat(caps.isGen1Supported()).isTrue();
        assertThat(caps.isGen2Supported()).isTrue();
    }

    @Test
    void testBothSupportedWhenCapabilitiesNull() {
        AzureVmCapabilities caps = new AzureVmCapabilities("Standard_D4s_v5", null);

        assertThat(caps.isGen1Supported()).isTrue();
        assertThat(caps.isGen2Supported()).isTrue();
    }

    private ResourceSkuCapabilities capability(String name, String value) {
        ResourceSkuCapabilities cap = mock(ResourceSkuCapabilities.class);
        when(cap.name()).thenReturn(name);
        when(cap.value()).thenReturn(value);
        return cap;
    }
}
