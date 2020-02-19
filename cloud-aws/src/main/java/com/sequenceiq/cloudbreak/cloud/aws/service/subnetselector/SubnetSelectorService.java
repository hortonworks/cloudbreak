package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

@Service
class SubnetSelectorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetSelectorService.class);

    Map<String, CloudSubnet> collectOnePrivateSubnetPerAz(List<CloudSubnet> subnetMetas, int max) {
        Map<String, CloudSubnet> subnetsPerAz = new HashMap<>();
        Iterator<CloudSubnet> subnetIterator = subnetMetas.iterator();
        while (subnetsPerAz.size() < max && subnetIterator.hasNext()) {
            CloudSubnet nextSubnet = subnetIterator.next();
            if (nextSubnet.isPrivateSubnet()) {
                subnetsPerAz.putIfAbsent(nextSubnet.getAvailabilityZone(), nextSubnet);
            }
        }
        LOGGER.debug("Private subnets per AZ: {}", subnetsPerAz.values().stream().map(CloudSubnet::getId).collect(Collectors.joining(",")));
        return subnetsPerAz;
    }

    Optional<CloudSubnet> getOnePrivateSubnet(List<CloudSubnet> subnetMetas) {
        Optional<CloudSubnet> foundCloudSubnet = subnetMetas.stream()
                .filter(CloudSubnet::isPrivateSubnet)
                .findFirst();
        LOGGER.debug("Found private subnet: {}", foundCloudSubnet.map(CloudSubnet::getId).orElse("Not found"));
        return foundCloudSubnet;
    }

    Map<String, CloudSubnet> collectOnePublicSubnetPerAz(List<CloudSubnet> subnetMetas, int max) {
        Map<String, CloudSubnet> subnetsPerAz = new HashMap<>();
        Iterator<CloudSubnet> subnetIterator = subnetMetas.iterator();
        while (subnetsPerAz.size() < max && subnetIterator.hasNext()) {
            CloudSubnet nextSubnet = subnetIterator.next();
            if (isUsablePublicSubnet(nextSubnet)) {
                subnetsPerAz.putIfAbsent(nextSubnet.getAvailabilityZone(), nextSubnet);
            }
        }
        LOGGER.debug("Public subnets per AZ: {}", subnetsPerAz.values().stream().map(CloudSubnet::getId).collect(Collectors.joining(",")));
        return subnetsPerAz;
    }

    Optional<CloudSubnet> getOnePublicSubnet(List<CloudSubnet> subnetMetas) {
        Optional<CloudSubnet> foundCloudSubnet = subnetMetas.stream()
                .filter(this::isUsablePublicSubnet)
                .findFirst();
        LOGGER.debug("Found public subnet: {}", foundCloudSubnet.map(CloudSubnet::getId).orElse("Not found"));
        return foundCloudSubnet;
    }

    Map<String, CloudSubnet> collectSubnetsOfMissingAz(
            Map<String, CloudSubnet> selectedSubnetsPerAz,
            Map<String, CloudSubnet> additionalSubnetsPerAz,
            int max) {
        Iterator<CloudSubnet> subnetIterator = additionalSubnetsPerAz.values().iterator();
        while (selectedSubnetsPerAz.size() < max && subnetIterator.hasNext()) {
            CloudSubnet nextSubnet = subnetIterator.next();
            selectedSubnetsPerAz.putIfAbsent(nextSubnet.getAvailabilityZone(), nextSubnet);
        }
        return selectedSubnetsPerAz;
    }

    private boolean isUsablePublicSubnet(CloudSubnet sm) {
        return !sm.isPrivateSubnet() && sm.isMapPublicIpOnLaunch();
    }
}
