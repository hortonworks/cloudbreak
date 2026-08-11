package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureInstanceView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;

@Component
public class AzureVirtualMachineTypeProvider {

    public Set<String> getVmTypes(AzureStackView azureStackView) {
        return azureStackView.getInstancesByGroupType().values().stream()
                .flatMap(Collection::stream)
                .map(this::getFlavor)
                .collect(Collectors.toSet());
    }

    private String getFlavor(AzureInstanceView azureInstanceView) {
        return Optional.ofNullable(azureInstanceView)
                .map(AzureInstanceView::getFlavor)
                .orElseThrow(() -> new IllegalArgumentException("Instance flavor is missing."));
    }
}
