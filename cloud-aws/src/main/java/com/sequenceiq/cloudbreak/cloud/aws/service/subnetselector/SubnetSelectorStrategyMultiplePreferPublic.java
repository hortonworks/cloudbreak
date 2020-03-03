package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

@Component
public class SubnetSelectorStrategyMultiplePreferPublic extends SubnetSelectorStrategy {

    @VisibleForTesting
    @Value("${cb.aws.subnet.different.az.min:2}")
    int minSubnetCountInDifferentAz;

    @VisibleForTesting
    @Value("${cb.aws.subnet.different.az.max:3}")
    int maxSubnetCountInDifferentAz;

    @Inject
    private SubnetSelectorService subnetSelectorService;

    @Override
    protected SubnetSelectionResult selectInternal(List<CloudSubnet> subnetMetas) {
        Map<String, CloudSubnet> selectedSubnets = subnetSelectorService.collectOnePublicSubnetPerAz(subnetMetas, maxSubnetCountInDifferentAz);
        if (selectedSubnets.size() < minSubnetCountInDifferentAz) {
            Map<String, CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectOnePrivateSubnetPerAz(subnetMetas, maxSubnetCountInDifferentAz);
            subnetSelectorService.collectSubnetsOfMissingAz(selectedSubnets, privateSubnetsPerAz,
                    minSubnetCountInDifferentAz);
            if (selectedSubnets.size() < minSubnetCountInDifferentAz) {
                return new SubnetSelectionResult(String.format(NOT_ENOUGH_AZ, selectedSubnets.size(), minSubnetCountInDifferentAz));
            }
        }
        return new SubnetSelectionResult(new ArrayList<>(selectedSubnets.values()));
    }

    @Override
    public SubnetSelectorStrategyType getType() {
        return SubnetSelectorStrategyType.MULTIPLE_PREFER_PUBLIC;
    }

    @Override
    protected int getMinimumNumberOfSubnets() {
        return minSubnetCountInDifferentAz;
    }
}
