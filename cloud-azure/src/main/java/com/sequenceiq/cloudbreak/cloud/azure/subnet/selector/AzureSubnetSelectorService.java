package com.sequenceiq.cloudbreak.cloud.azure.subnet.selector;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;

@Service
public class AzureSubnetSelectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureSubnetSelectorService.class);

    public List<CloudSubnet> select(List<CloudSubnet> subnetMetas, SubnetSelectionParameters subnetSelectionParameters) {
        if (subnetMetas == null || subnetMetas.isEmpty()) {
            return logAndThrow("Azure subnet selection: there are no subnets to choose from.");
        }
        if (subnetSelectionParameters == null) {
            return logAndThrow("Azure subnet selection: parameters were not specified.");
        }
        return subnetSelectionParameters.isForDatabase()
                ? new ArrayList<>(subnetMetas)
                : List.of(subnetMetas.get(0));
    }

    private List<CloudSubnet> logAndThrow(String message) {
        LOGGER.debug(message);
        throw new BadRequestException(message);
    }
}
