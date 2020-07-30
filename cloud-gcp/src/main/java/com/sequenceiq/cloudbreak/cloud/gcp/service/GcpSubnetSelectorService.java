package com.sequenceiq.cloudbreak.cloud.gcp.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

@Service
public class GcpSubnetSelectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpSubnetSelectorService.class);

    public SubnetSelectionResult select(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        Optional<String> errorMessage = quickValidate(subnetMetas, subnetSelectionParameters);
        if (errorMessage.isPresent()) {
            LOGGER.debug("{}", errorMessage.get());
            return new SubnetSelectionResult(errorMessage.get());
        }
        // GCP VPCs are global and different subnets can be in different geographies. So for safety just choosing the first subnet.
        CloudSubnet first = subnetMetas.stream().findFirst().get();
        LOGGER.debug("GCP selected subnet: '{}'", first);
        return new SubnetSelectionResult(Collections.singletonList(first));
    }

    private Optional<String> quickValidate(Collection<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        if (subnetMetas == null || subnetMetas.isEmpty()) {
            return Optional.of("GCP subnet selection: there are no subnets to choose from.");
        }
        if (subnetSelectionParameters == null) {
            return Optional.of("GCP subnet selection: parameters were not specified.");
        }
        return Optional.empty();
    }
}
