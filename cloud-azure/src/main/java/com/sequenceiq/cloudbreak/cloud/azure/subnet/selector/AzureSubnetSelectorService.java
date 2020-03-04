package com.sequenceiq.cloudbreak.cloud.azure.subnet.selector;

import java.util.ArrayList;
import java.util.List;

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
        if (subnetMetas == null || subnetMetas.isEmpty()) {
            return logResult(new SubnetSelectionResult("Azure subnet selection: there are no subnets to choose from."));
        }
        if (subnetSelectionParameters == null) {
            return logResult(new SubnetSelectionResult("Azure subnet selection: parameters were not specified."));
        }
        List<CloudSubnet> selectedSubnets = subnetSelectionParameters.isForDatabase()
                ? new ArrayList<>(subnetMetas)
                : List.of(subnetMetas.get(0));
        return logResult(new SubnetSelectionResult(selectedSubnets));
    }

    private SubnetSelectionResult logResult(SubnetSelectionResult subnetSelectionResult) {
        if (subnetSelectionResult.hasError()) {
            LOGGER.debug("{}", subnetSelectionResult.getErrorMessage());
        } else {
            LOGGER.debug("Azure selected subnets: '{}'", String.join(", ", subnetSelectionResult.getResult().stream().toString()));
        }
        return subnetSelectionResult;
    }
}
