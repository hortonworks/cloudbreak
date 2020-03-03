package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

public abstract class SubnetSelectorStrategy {

    static final String NOT_ENOUGH_AZ = "Acceptable subnets are in %d different AZs, but subnets in %d different AZs required.";

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetSelectorStrategy.class);

    public SubnetSelectionResult select(List<CloudSubnet> subnetMetas) {
        LOGGER.debug("Subnet selection with strategy '{}'", getType());
        Optional<String> errorMessage = quickValidate(subnetMetas);
        if (errorMessage.isPresent()) {
            return new SubnetSelectionResult(errorMessage.get());
        }
        SubnetSelectionResult selectionResult = selectInternal(subnetMetas);
        logResult(selectionResult);
        return selectionResult;
    }

    protected abstract SubnetSelectionResult selectInternal(List<CloudSubnet> subnets);

    public abstract SubnetSelectorStrategyType getType();

    protected abstract int getMinimumNumberOfSubnets();

    private Optional<String> quickValidate(List<CloudSubnet> subnetMetas) {
        if (subnetMetas == null || subnetMetas.isEmpty()) {
            return Optional.of("There are no subnets in this network.");
        }
        if (subnetMetas.size() < getMinimumNumberOfSubnets()) {
            return Optional.of(String.format("There are not enough subnets in this network, found: %d, expected: %d.",
                    subnetMetas.size(), getMinimumNumberOfSubnets()));
        }
        return Optional.empty();
    }

    private void logResult(SubnetSelectionResult selectionResult) {
        if (selectionResult.hasError()) {
            LOGGER.debug("Subnet selection returned with error: '{}'", selectionResult.getErrorMessage());
        } else {
            LOGGER.debug("Selected subnets: '{}'", String.join(", ", selectionResult.getResult().stream().toString()));
        }
    }

    String formatErrorNoSuitableSubnets(List<CloudSubnet> subnetMetas) {
        return String.format(
                "No suitable subnet found as there were neither private nor any suitable public subnets in '%s'.",
                subnetMetas.stream().map(CloudSubnet::getId).collect(Collectors.joining(", "))
        );
    }
}
