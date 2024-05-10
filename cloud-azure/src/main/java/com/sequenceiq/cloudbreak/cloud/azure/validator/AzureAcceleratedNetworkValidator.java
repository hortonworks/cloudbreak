package com.sequenceiq.cloudbreak.cloud.azure.validator;

import static com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta.ENHANCED_NETWORK;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.AzureVmCapabilities;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureVirtualMachineTypeProvider;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.model.VmType;

@Component
public class AzureAcceleratedNetworkValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureAcceleratedNetworkValidator.class);

    @Inject
    private AzureVirtualMachineTypeProvider azureVirtualMachineTypeProvider;

    public Map<String, Boolean> validate(AzureStackView azureStackView, Set<VmType> vmTypes) {
        return getVmFlavors(azureStackView).stream().collect(Collectors.toMap(vm -> vm,
                vm -> doesVmTypeSupportEnhancedNetworking(vmTypes, vm)));
    }

    private boolean doesVmTypeSupportEnhancedNetworking(Set<VmType> vmTypes, String vm) {
        return vmTypes.stream()
                .filter(vmType -> vmType.value().equalsIgnoreCase(vm))
                .map(vmType -> (Boolean) vmType.getMetaData().getProperties().getOrDefault(ENHANCED_NETWORK, Boolean.FALSE))
                .findFirst().orElse(Boolean.FALSE);
    }

    private Set<String> getVmFlavors(AzureStackView azureStackView) {
        return azureVirtualMachineTypeProvider.getVmTypes(azureStackView);
    }

    public boolean isSupportedForVm(String vmType, Map<String, AzureVmCapabilities> azureVmCapabilities) {
        LOGGER.trace("Validating vm type: " + vmType);
        return azureVmCapabilities.getOrDefault(vmType, new AzureVmCapabilities(vmType, List.of())).isAcceleratedNetworkingEnabled();
    }
}
