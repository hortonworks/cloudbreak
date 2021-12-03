package com.sequenceiq.cloudbreak.cloud.azure.util;

import com.sequenceiq.cloudbreak.cloud.azure.view.AzureInstanceView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AzureVirtualMachineTypeProvider {

    public Set<String> getVmTypes(AzureStackView azureStackView) {
        return azureStackView.getGroups().values().stream()
                .flatMap(Collection::stream)
                .map(this::getFlavour)
                .collect(Collectors.toSet());
    }

    private String getFlavour(AzureInstanceView azureInstanceView) {
        return Optional.of(azureInstanceView)
                .map(AzureInstanceView::getInstance)
                .map(CloudInstance::getTemplate)
                .map(InstanceTemplate::getFlavor)
                .orElseThrow(() -> new IllegalArgumentException("Instance favour is missing."));
    }
}
