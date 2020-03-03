package com.sequenceiq.cloudbreak.cloud.azure.subnet.selector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

@Service
public class AzureSubnetSelectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureSubnetSelectorService.class);

    public SubnetSelectionResult select(List<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        Optional<String> errorMessage = quickValidate(subnetMetas, subnetSelectionParameters);
        if (errorMessage.isPresent()) {
            LOGGER.debug("{}", errorMessage.get());
            return new SubnetSelectionResult(errorMessage.get());
        }

        List<CloudSubnet> selectedSubnets = subnetSelectionParameters.isForDatabase()
                ? new ArrayList<>(subnetMetas)
                : List.of(subnetMetas.get(0));
        LOGGER.debug("Azure selected subnets: '{}'", String.join(", ", selectedSubnets.stream().toString()));
        return new SubnetSelectionResult(selectedSubnets);
    }

    private Optional<String> quickValidate(List<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        if (subnetMetas == null || subnetMetas.isEmpty()) {
            return Optional.of("Azure subnet selection: there are no subnets to choose from.");
        }
        if (subnetSelectionParameters == null) {
            return Optional.of("Azure subnet selection: parameters were not specified.");
        }
        return Optional.empty();
    }
}
