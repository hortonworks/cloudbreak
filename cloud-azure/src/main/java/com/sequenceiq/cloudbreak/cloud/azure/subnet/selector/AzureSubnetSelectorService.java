package com.sequenceiq.cloudbreak.cloud.azure.subnet.selector;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

@Service
public class AzureSubnetSelectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureSubnetSelectorService.class);

    public SubnetSelectionResult select(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        Optional<String> errorMessage = quickValidate(subnetMetas, subnetSelectionParameters);
        if (errorMessage.isPresent()) {
            LOGGER.debug("{}", errorMessage.get());
            return new SubnetSelectionResult(errorMessage.get());
        }
        LOGGER.debug("Azure selected subnets: '{}'", String.join(", ", subnetMetas.stream().toString()));
        return new SubnetSelectionResult(subnetMetas.stream().collect(Collectors.toList()));
    }

    private Optional<String> quickValidate(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        if (subnetMetas == null || subnetMetas.isEmpty()) {
            return Optional.of("Azure subnet selection: there are no subnets to choose from.");
        }
        if (subnetSelectionParameters == null) {
            return Optional.of("Azure subnet selection: parameters were not specified.");
        }
        return Optional.empty();
    }
}
