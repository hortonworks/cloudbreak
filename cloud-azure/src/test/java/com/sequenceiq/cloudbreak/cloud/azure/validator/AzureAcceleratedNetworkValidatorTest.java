package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.azure.resourcemanager.compute.models.ResourceSkuCapabilities;
import com.sequenceiq.cloudbreak.cloud.azure.AzureVmCapabilities;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureVirtualMachineTypeProvider;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureInstanceView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;

@ExtendWith(MockitoExtension.class)
public class AzureAcceleratedNetworkValidatorTest {

    @Mock
    private AzureVirtualMachineTypeProvider azureVirtualMachineTypeProvider;

    @InjectMocks
    private AzureAcceleratedNetworkValidator underTest;

    @Mock
    private AzureStackView azureStackView;

    private Set<VmType> vmTypes;

    private Map<String, AzureVmCapabilities> azureVmCapabilities;

    @BeforeEach
    public void setUp() {
        vmTypes = new HashSet<>();
        azureVmCapabilities = new HashMap<>();
    }

    @Test
    public void testValidateAllVmTypesSupportEnhancedNetworking() {
        String vmFlavor1 = "vmFlavor1";
        String vmFlavor2 = "vmFlavor2";
        VmTypeMeta meta1 = VmTypeMeta.VmTypeMetaBuilder.builder().withEnhancedNetwork(true).create();
        VmTypeMeta meta2 = VmTypeMeta.VmTypeMetaBuilder.builder().withEnhancedNetwork(true).create();
        vmTypes.add(VmType.vmTypeWithMeta(vmFlavor1, meta1, true));
        vmTypes.add(VmType.vmTypeWithMeta(vmFlavor2, meta2, true));
        when(azureVirtualMachineTypeProvider.getVmTypes(any())).thenReturn(Set.of(vmFlavor1, vmFlavor2));

        Map<String, Boolean> result = underTest.validate(azureStackView, vmTypes);

        assertEquals(2, result.size());
        assertTrue(result.get(vmFlavor1));
        assertTrue(result.get(vmFlavor2));
    }

    @Test
    public void testValidateSomeVmTypesDoNotSupportEnhancedNetworking() {
        String vmFlavor1 = "vmFlavor1";
        String vmFlavor2 = "vmFlavor2";
        VmTypeMeta meta1 = VmTypeMeta.VmTypeMetaBuilder.builder().withEnhancedNetwork(true).create();
        VmTypeMeta meta2 = VmTypeMeta.VmTypeMetaBuilder.builder().withEnhancedNetwork(false).create();
        vmTypes.add(VmType.vmTypeWithMeta(vmFlavor1, meta1, true));
        vmTypes.add(VmType.vmTypeWithMeta(vmFlavor2, meta2, true));
        when(azureVirtualMachineTypeProvider.getVmTypes(any())).thenReturn(Set.of(vmFlavor1, vmFlavor2));

        Map<String, Boolean> result = underTest.validate(azureStackView, vmTypes);

        assertEquals(2, result.size());
        assertTrue(result.get(vmFlavor1));
        assertFalse(result.get(vmFlavor2));
    }

    @Test
    public void testIsSupportedForVmAcceleratedNetworkingEnabled() {
        String vmType = "vmType";
        ResourceSkuCapabilities networkCapability = mock(ResourceSkuCapabilities.class);
        when(networkCapability.name()).thenReturn("AcceleratedNetworkingEnabled");
        when(networkCapability.value()).thenReturn("true");
        azureVmCapabilities.put(vmType, new AzureVmCapabilities(vmType, List.of(networkCapability)));

        assertTrue(underTest.isSupportedForVm(vmType, azureVmCapabilities));
    }

    @Test
    public void testIsSupportedForVmAcceleratedNetworkingDisabled() {
        String vmType = "vmType";
        azureVmCapabilities.put(vmType, new AzureVmCapabilities(vmType, List.of()));

        assertFalse(underTest.isSupportedForVm(vmType, azureVmCapabilities));
    }

    @Test
    public void testIsSupportedForVmAcceleratedNetworkingCapabilityNotAvailable() {
        String vmType = "vmType";

        assertFalse(underTest.isSupportedForVm(vmType, azureVmCapabilities));
    }

    @Test
    public void testValidateUsesOverriddenFlavorSoTheMapMatchesTheTemplateLookup() {
        AzureAcceleratedNetworkValidator withRealProvider = new AzureAcceleratedNetworkValidator();
        ReflectionTestUtils.setField(withRealProvider, "azureVirtualMachineTypeProvider",
                new AzureVirtualMachineTypeProvider());
        String original = "Standard_D8_v3";
        String override = "Standard_D16_v3";
        InstanceTemplate template = new InstanceTemplate(original, "master", null,
                Collections.emptyList(), null, Collections.emptyMap(), null, null, TemporaryStorage.ATTACHED_VOLUMES, 0L);
        CloudInstance instance = new CloudInstance(null, template, null, "subnet-1", "az1");
        AzureInstanceView overridden = AzureInstanceView.builder(instance).build()
                .toBuilder().withFlavorOverride(override).build();
        Map<String, List<AzureInstanceView>> instanceGroups = Map.of("CORE", List.of(overridden));
        AzureStackView stackView = mock(AzureStackView.class);
        when(stackView.getInstancesByGroupType()).thenReturn(instanceGroups);
        VmTypeMeta meta = VmTypeMeta.VmTypeMetaBuilder.builder().withEnhancedNetwork(true).create();
        vmTypes.add(VmType.vmTypeWithMeta(override, meta, true));

        Map<String, Boolean> result = withRealProvider.validate(stackView, vmTypes);

        assertTrue(result.containsKey(override), "acceleratedNetworkEnabled map must be keyed by the overridden flavor");
        assertFalse(result.containsKey(original), "acceleratedNetworkEnabled map must NOT contain the original flavor after override");
    }
}
