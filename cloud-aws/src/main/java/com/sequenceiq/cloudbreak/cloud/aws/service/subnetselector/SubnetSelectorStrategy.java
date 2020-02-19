package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

public abstract class SubnetSelectorStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetSelectorStrategy.class);

    public List<CloudSubnet> select(List<CloudSubnet> subnetMetas) {
        LOGGER.debug("Subnet selection with strategy '{}'", getType());
        quickValidate(subnetMetas);
        List<CloudSubnet> selectedNetworks = selectInternal(subnetMetas);
        LOGGER.debug("Selected subnets: {}", String.join(", ", selectedNetworks.stream().toString()));
        return selectedNetworks;
    }

    private void quickValidate(List<CloudSubnet> subnetMetas) {
        if (subnetMetas == null || subnetMetas.isEmpty()) {
            error("There are no subnets in this network.");
        }
        if (subnetMetas.size() < getMinimumNumberOfSubnets()) {
            error(String.format("There are not enough subnets in this network, found: %d, expected: %d.", subnetMetas.size(), getMinimumNumberOfSubnets()));
        }
    }

    protected abstract List<CloudSubnet> selectInternal(List<CloudSubnet> subnets);

    public abstract SubnetSelectorStrategyType getType();

    protected abstract int getMinimumNumberOfSubnets();

    protected void error(String message) {
        LOGGER.debug("Error when selecting subnets with strategy '{}': {}", getType(), message);
        throw new BadRequestException(String.format("Error when selecting subnets with strategy '%s': %s", getType().getDescription(), message));
    }

    void errorNotEnoughAZs(int found, int expected) {
        error(String.format("Acceptable subnets are in %d different AZs, but subnets in %d different AZs required.", found, expected));

    }

    void errorNoSuitableSubnets(List<CloudSubnet> subnetMetas) {
        error(String.format("No suitable subnet found as there were neither private nor any suitable public subnets in '%s'.",
                subnetMetas.stream().map(CloudSubnet::getId).collect(Collectors.joining(", "))));

    }
}
