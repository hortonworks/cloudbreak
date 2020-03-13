package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

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
public class SubnetFilterStrategyMultiplePreferPrivate extends SubnetFilterStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetFilterStrategyMultiplePreferPrivate.class);

    @Inject
    private SubnetSelectorService subnetSelectorService;

    @Override
    public SubnetSelectionResult filter(Collection<CloudSubnet> subnets, int azCount) {
        List<CloudSubnet> result = subnetSelectorService.collectPrivateSubnets(subnets);
        Set<String> uniqueAzs = result.stream()
                .map(e -> e.getAvailabilityZone())
                .collect(Collectors.toSet());
        if (uniqueAzs.size() < azCount) {
            LOGGER.info("There is not enough different AZ in the private subnets which {}, falling back to private subnets: {}",
                    uniqueAzs.size(), subnets);
            List<CloudSubnet> publicSubnets = subnetSelectorService.collectPublicSubnets(subnets);
            for (CloudSubnet publicSubnet : publicSubnets) {
                if (!uniqueAzs.contains(publicSubnet.getAvailabilityZone())) {
                    result.add(publicSubnet);
                    uniqueAzs.add(publicSubnet.getAvailabilityZone());
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
        return SubnetFilterStrategyType.MULTIPLE_PREFER_PRIVATE;
    }
}
