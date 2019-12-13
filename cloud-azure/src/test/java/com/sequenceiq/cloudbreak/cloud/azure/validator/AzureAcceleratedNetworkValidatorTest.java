package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureVirtualMachineTypeProvider;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@RunWith(MockitoJUnitRunner.class)
public class AzureAcceleratedNetworkValidatorTest {

    @Mock
    private AzureVirtualMachineTypeProvider azureVirtualMachineTypeProvider;

    @Mock
    private AzureStackView azureStackView;

    @InjectMocks
    private AzureAcceleratedNetworkValidator underTest;

    @Test
    public void testIsvSupportedForVmShouldReturnsWithTrueForTheGivenVmTypes() throws IOException {
        Set<String> supportedVmTypes = getSupportedVmTypes();
        when(azureVirtualMachineTypeProvider.getVmTypes(azureStackView)).thenReturn(supportedVmTypes);

        Map<String, Boolean> actual = underTest.validate(azureStackView);

        actual.forEach((key, value) -> Assert.assertTrue(value));
    }

    @Test
    public void testValidateShouldReturnsWithFalseForTheGivenVmTypesWhenOnlyTwoCoreAvailable() {
        Set<String> supportedVmTypes = getVmsWithTwoCore();
        when(azureVirtualMachineTypeProvider.getVmTypes(azureStackView)).thenReturn(supportedVmTypes);

        Map<String, Boolean> actual = underTest.validate(azureStackView);

        actual.forEach((key, value) -> Assert.assertFalse(value));
    }

    @Test
    public void testValidateShouldReturnsWithFalseForTheGivenVmTypesWhenCoreNumberIsNotFound() {
        Set<String> supportedVmTypes = getVmsWithoutCpuCoreInfo();
        when(azureVirtualMachineTypeProvider.getVmTypes(azureStackView)).thenReturn(supportedVmTypes);

        Map<String, Boolean> actual = underTest.validate(azureStackView);

        actual.forEach((key, value) -> Assert.assertFalse(value));
    }

    @Test
    public void testValidateShouldReturnsWithFalseForOtherVmTypes() {
        Set<String> supportedVmTypes = getOtherVms();
        when(azureVirtualMachineTypeProvider.getVmTypes(azureStackView)).thenReturn(supportedVmTypes);

        Map<String, Boolean> actual = underTest.validate(azureStackView);

        actual.forEach((key, value) -> Assert.assertFalse(value));
    }

    private Set<String> getSupportedVmTypes() throws IOException {
        return JsonUtil.readValue(FileReaderUtils.readFileFromClasspath("/json/azure-vms.json"), new TypeReference<Set<String>>() {
        });
    }

    private Set<String> getVmsWithTwoCore() {
        return Set.of("Standard_DS2_v2", "Standard_F2s", "Standard_F2", "Standard_D2_v2_Promo", "Standard_DS2_v2_Promo", "Standard_D2_v2");
    }

    private Set<String> getVmsWithoutCpuCoreInfo() {
        return Set.of("Standard_DS_v2", "Standard_Fs", "Standard_F", "Standard_D_v2_Promo", "Standard_DS_v2_Promo", "Standard_D", "FCA_E1-14s_v3");
    }

    private Set<String> getOtherVms() {
        return Set.of("E4_Flex", "SQLG5_IaaS", "AZAP_Performance_ComputeV17C_12", "SQLG5-80m", "SQLG6", "SQLDCGen6_2");
    }

}