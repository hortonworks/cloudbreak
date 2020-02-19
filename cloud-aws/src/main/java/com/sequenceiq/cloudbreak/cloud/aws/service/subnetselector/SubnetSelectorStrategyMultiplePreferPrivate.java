package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

@Component
public class SubnetSelectorStrategyMultiplePreferPrivate extends SubnetSelectorStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetSelectorStrategyMultiplePreferPrivate.class);

    @VisibleForTesting
    @Value("${cb.aws.subnet.different.az.min:2}")
    int minSubnetCountInDifferentAz;

    @VisibleForTesting
    @Value("${cb.aws.subnet.different.az.max:3}")
    int maxSubnetCountInDifferentAz;

    @Inject
    private SubnetSelectorService subnetSelectorService;

    @Override
    public List<CloudSubnet> selectInternal(List<CloudSubnet> subnetMetas) {
        Map<String, CloudSubnet> selectedSubnets = subnetSelectorService.collectOnePrivateSubnetPerAz(subnetMetas, maxSubnetCountInDifferentAz);
        if (selectedSubnets.size() < minSubnetCountInDifferentAz) {
            Map<String, CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectOnePublicSubnetPerAz(subnetMetas, minSubnetCountInDifferentAz);
            subnetSelectorService.collectSubnetsOfMissingAz(selectedSubnets, publicSubnetsPerAz, minSubnetCountInDifferentAz);
            if (selectedSubnets.size() < minSubnetCountInDifferentAz) {
                errorNotEnoughAZs(selectedSubnets.size(), minSubnetCountInDifferentAz);
            }
        }
        return new ArrayList<>(selectedSubnets.values());
    }

    @Override
    public SubnetSelectorStrategyType getType() {
        return SubnetSelectorStrategyType.MULTIPLE_PREFER_PRIVATE;
    }

    @Override
    protected int getMinimumNumberOfSubnets() {
        return minSubnetCountInDifferentAz;
    }
}
