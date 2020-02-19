package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

@Component
public class SubnetSelectorStrategySinglePreferPublic extends SubnetSelectorStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetSelectorStrategySinglePreferPublic.class);

    @Inject
    private SubnetSelectorService subnetSelectorService;

    @Override
    public List<CloudSubnet> selectInternal(List<CloudSubnet> subnetMetas) {
        Optional<CloudSubnet> foundSubnet = subnetSelectorService.getOnePublicSubnet(subnetMetas);
        if (foundSubnet.isEmpty()) {
            foundSubnet = subnetSelectorService.getOnePrivateSubnet(subnetMetas);
            if (foundSubnet.isEmpty()) {
                errorNoSuitableSubnets(subnetMetas);
            }
        }
        return List.of(foundSubnet.get());
    }

    @Override
    public SubnetSelectorStrategyType getType() {
        return SubnetSelectorStrategyType.SINGLE_PREFER_PUBLIC;
    }

    @Override
    protected int getMinimumNumberOfSubnets() {
        return 1;
    }
}
