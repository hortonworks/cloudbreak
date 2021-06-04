package com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

@Component
public class SubnetFilterStrategyMultiplePreferPublic implements SubnetFilterStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetFilterStrategyMultiplePreferPublic.class);

    @Inject
    private SubnetSelectorService subnetSelectorService;

    @Override
    public SubnetSelectionResult filter(Collection<CloudSubnet> subnets, int azCount) {
        List<CloudSubnet> result = subnetSelectorService.collectPublicSubnets(subnets);
        Set<String> uniqueAzs = result.stream().map(e -> e.getAvailabilityZone()).collect(Collectors.toSet());
        if (uniqueAzs.size() < azCount) {
            LOGGER.info("There is not enough different AZ in the public subnets which {}, falling back to private subnets: {}",
                    uniqueAzs.size(), subnets);
            List<CloudSubnet> privateSubnets = subnetSelectorService.collectPrivateSubnets(subnets);
            for (CloudSubnet privateSubnet : privateSubnets) {
                if (!uniqueAzs.contains(privateSubnet.getAvailabilityZone())) {
                    result.add(privateSubnet);
                    uniqueAzs.add(privateSubnet.getAvailabilityZone());
                    if (uniqueAzs.size() >= azCount) {
                        break;
                    }
                }
            }
        }
        return new SubnetSelectionResult(result);
    }

    @Override
    public SubnetFilterStrategyType getType() {
        return SubnetFilterStrategyType.MULTIPLE_PREFER_PUBLIC;
    }
}
