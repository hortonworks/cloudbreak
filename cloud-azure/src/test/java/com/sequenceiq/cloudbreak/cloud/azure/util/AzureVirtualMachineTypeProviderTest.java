package com.sequenceiq.cloudbreak.cloud.azure.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureInstanceView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public class AzureVirtualMachineTypeProviderTest {

    private static final String STANDARD_DS_V2 = "Standard_DS_v2";

    private static final String STANDARD_D3_V2 = "Standard_D3_v2";

    private static final String STANDARD_E16A_V3 = "Standard_E16a_v3";

    private static final String STANDARD_D15_V2 = "Standard_D15_v2";

    private AzureVirtualMachineTypeProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new AzureVirtualMachineTypeProvider();
    }

    @Test
    public void testGetVmTypesShouldReturnsTheAvailableVmTypes() {
        Map<String, List<AzureInstanceView>> instanceGroups = createInstanceGroups();
        AzureStackView azureStackView = createAzureStackView(instanceGroups);

        Set<String> actual = underTest.getVmTypes(azureStackView);

        assertEquals(4, actual.size());
        assertTrue(actual.contains(STANDARD_DS_V2));
        assertTrue(actual.contains(STANDARD_D3_V2));
        assertTrue(actual.contains(STANDARD_E16A_V3));
        assertTrue(actual.contains(STANDARD_D15_V2));
    }

    @Test
    public void testGetVmTypesShouldThrowExceptionWhenAFlavourIsMissing() {
        Map<String, List<AzureInstanceView>> instanceGroups = createInstanceGroupsWithAMissingFlavour();
        AzureStackView azureStackView = createAzureStackView(instanceGroups);

        assertThrows(IllegalArgumentException.class, () -> underTest.getVmTypes(azureStackView));
    }

    private AzureStackView createAzureStackView(Map<String, List<AzureInstanceView>> instanceGroups) {
        AzureStackView azureStackView = mock(AzureStackView.class);
        when(azureStackView.getGroups()).thenReturn(instanceGroups);
        return azureStackView;
    }

    private Map<String, List<AzureInstanceView>> createInstanceGroups() {
        List<AzureInstanceView> coreInstances = List.of(
                createAzureInstanceView("leader", STANDARD_DS_V2),
                createAzureInstanceView("master", STANDARD_E16A_V3),
                createAzureInstanceView("worker", STANDARD_D15_V2));

        List<AzureInstanceView> gatewayInstances = List.of(
                createAzureInstanceView("gateway", STANDARD_D3_V2));
        return Map.of("CORE", coreInstances, "GATEWAY", gatewayInstances);
    }

    private Map<String, List<AzureInstanceView>> createInstanceGroupsWithAMissingFlavour() {
        List<AzureInstanceView> coreInstances = List.of(
                createAzureInstanceView("leader", null),
                createAzureInstanceView("master", STANDARD_E16A_V3),
                createAzureInstanceView("worker", STANDARD_D15_V2));

        List<AzureInstanceView> gatewayInstances = List.of(
                createAzureInstanceView("gateway", STANDARD_D3_V2));
        return Map.of("CORE", coreInstances, "GATEWAY", gatewayInstances);
    }

    private AzureInstanceView createAzureInstanceView(String groupName, String flavour) {
        InstanceTemplate instanceTemplate = new InstanceTemplate(flavour, groupName, null,
                Collections.emptyList(), null, Collections.emptyMap(), null, null);
        CloudInstance cloudInstance = new CloudInstance(null, instanceTemplate, null);
        return AzureInstanceView.builder(cloudInstance).build();
    }

}