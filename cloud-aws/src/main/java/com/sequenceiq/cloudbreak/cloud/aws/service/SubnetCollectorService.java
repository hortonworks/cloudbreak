package com.sequenceiq.cloudbreak.cloud.aws.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

@Service
public class SubnetCollectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetCollectorService.class);

    public List<CloudSubnet> collectPublicSubnets(Collection<CloudSubnet> subnetMetas) {
        List<CloudSubnet> result = new ArrayList<>();
        for (CloudSubnet subnetMeta : subnetMetas) {
            if (isUsablePublicSubnet(subnetMeta)) {
                result.add(subnetMeta);
            }
        }
        LOGGER.debug("Public subnets for selections: {}", result);
        return result;
    }

    public List<CloudSubnet> collectPrivateSubnets(Collection<CloudSubnet> subnetMetas) {
        List<CloudSubnet> result = new ArrayList<>();
        for (CloudSubnet subnetMeta : subnetMetas) {
            if (subnetMeta.isPrivateSubnet()) {
                result.add(subnetMeta);
            }
        }
        LOGGER.debug("Private subnets for selections: {}", result);
        return result;
    }

    private boolean isUsablePublicSubnet(CloudSubnet sm) {
        LOGGER.info("The current subnet is: {}", sm);
        return !sm.isPrivateSubnet() && sm.isMapPublicIpOnLaunch();
    }
}
