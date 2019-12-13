package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureInstanceView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

public class AzureVirtualMachineTypeProviderTest {

    private static final String STANDARD_DS_V2 = "Standard_DS_v2";

    private static final String STANDARD_D3_V2 = "Standard_D3_v2";

    private static final String STANDARD_E16A_V3 = "Standard_E16a_v3";

    private static final String STANDARD_D15_V2 = "Standard_D15_v2";

    private AzureVirtualMachineTypeProvider underTest = new AzureVirtualMachineTypeProvider();

    @Test
    public void testGetVmTypesShouldReturnsTheAvailableVmTypes() {
        Map<String, List<AzureInstanceView>> instanceGroups = createInstanceGroups();
        AzureStackView azureStackView = createAzureStackView(instanceGroups);

        Set<String> actual = underTest.getVmTypes(azureStackView);

        Assert.assertEquals(4, actual.size());
        Assert.assertTrue(actual.contains(STANDARD_DS_V2));
        Assert.assertTrue(actual.contains(STANDARD_D3_V2));
        Assert.assertTrue(actual.contains(STANDARD_E16A_V3));
        Assert.assertTrue(actual.contains(STANDARD_D15_V2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetVmTypesShouldThrowExceptionWhenAFlavourIsMissing() {
        Map<String, List<AzureInstanceView>> instanceGroups = createInstanceGroupsWithAMissingFlavour();
        AzureStackView azureStackView = createAzureStackView(instanceGroups);

        underTest.getVmTypes(azureStackView);
    }

    private AzureStackView createAzureStackView(Map<String, List<AzureInstanceView>> instanceGroups) {
        AzureStackView azureStackView = Mockito.mock(AzureStackView.class);
        Mockito.when(azureStackView.getGroups()).thenReturn(instanceGroups);
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
        return new AzureInstanceView(null, 0, cloudInstance, null, null, null,
                null, null, false, null, 0, null, null);
    }
}