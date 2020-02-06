package com.sequenceiq.redbeams.service.network;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;

@Component
public class AwsSubnetChooser {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSubnetChooser.class);

    private static final int MIN_SUBNET_NO_MULTI_AZ = 1;

    private static final int MIN_SUBNET_IN_DIFFERENT_AZ = 2;

    private static final int MAX_SUBNET_IN_DIFFERENT_AZ = 3;

    @Inject
    private AwsSubnetValidator awsSubnetValidator;

    public List<CloudSubnet> chooseSubnets(List<CloudSubnet> subnetMetas, Map<String, String> dbParameters) {
        LOGGER.debug("Subnets to choose from: [{}]", subnetMetas);
        int minSubnetInDifferentAz = determineMinSubnetInDifferentAz(dbParameters);
        awsSubnetValidator.validate(subnetMetas, minSubnetInDifferentAz);
        return chooseAwsSubnetsInDifferentAz(subnetMetas, minSubnetInDifferentAz);
    }

    private int determineMinSubnetInDifferentAz(Map<String, String> dbParameters) {
        return dbParameters != null && Boolean.FALSE.toString().equalsIgnoreCase(dbParameters.get(AwsDatabaseServerV4Parameters.MULTI_AZ))
                ? MIN_SUBNET_NO_MULTI_AZ : MIN_SUBNET_IN_DIFFERENT_AZ;
    }

    private List<CloudSubnet> chooseAwsSubnetsInDifferentAz(List<CloudSubnet> subnetMetas, int minSubnetInDifferentAz) {
        Map<String, Collection<CloudSubnet>> privateSubnetPerAz = collectPrivateSubnetPerAz(subnetMetas);
        Set<CloudSubnet> privateSubnetsInDifferentAz = findMaxThreePrivateSubnetInDiffrentAz(privateSubnetPerAz);
        List<CloudSubnet> chosenSubnets = new ArrayList<>(privateSubnetsInDifferentAz);
        chosenSubnets.addAll(collectPublicSubnetsIfNeeded(subnetMetas, privateSubnetPerAz, chosenSubnets.size(), minSubnetInDifferentAz));
        LOGGER.debug("Chosen subnets: [{}]", chosenSubnets);
        return chosenSubnets;
    }

    private Set<CloudSubnet> collectPublicSubnetsIfNeeded(List<CloudSubnet> subnetMetas, Map<String, Collection<CloudSubnet>> privateSubnetPerAz,
            int chosenSubnetSize, int minSubnetInDifferentAz) {
        if (chosenSubnetSize < minSubnetInDifferentAz) {
            Map<String, Collection<CloudSubnet>> publicSubnetPerAz = mapPublicSubnetPerAz(subnetMetas, privateSubnetPerAz);
            return collectLimitedPublicSubnetsInDifferentAz(chosenSubnetSize, publicSubnetPerAz, minSubnetInDifferentAz);
        } else {
            LOGGER.debug("No public subnets needed");
            return Set.of();
        }
    }

    private Set<CloudSubnet> collectLimitedPublicSubnetsInDifferentAz(int chosenSubnetSize, Map<String, Collection<CloudSubnet>> publicSubnetPerAz,
            int minSubnetInDifferentAz) {
        Set<CloudSubnet> chosenPublicSubnets = publicSubnetPerAz.values().stream()
                .map(cloudSubnets -> cloudSubnets.stream().findFirst().get())
                .limit(minSubnetInDifferentAz - chosenSubnetSize)
                .collect(Collectors.toSet());
        LOGGER.debug("Chosen public subnets: [{}]", chosenPublicSubnets);
        return chosenPublicSubnets;
    }

    private Map<String, Collection<CloudSubnet>> mapPublicSubnetPerAz(List<CloudSubnet> subnetMetas, Map<String, Collection<CloudSubnet>> privateSubnetPerAz) {
        return subnetMetas.stream()
                .filter(sm -> !sm.isPrivateSubnet())
                .filter(sm -> !privateSubnetPerAz.containsKey(sm.getAvailabilityZone()))
                .collect(Multimaps.toMultimap(CloudSubnet::getAvailabilityZone, sm -> sm, ArrayListMultimap::create)).asMap();
    }

    private Set<CloudSubnet> findMaxThreePrivateSubnetInDiffrentAz(Map<String, Collection<CloudSubnet>> privateSubnetPerAz) {
        Set<CloudSubnet> chosenPrivateSubnets = privateSubnetPerAz.values().stream()
                .map(cloudSubnets -> cloudSubnets.stream().findFirst().get())
                .limit(MAX_SUBNET_IN_DIFFERENT_AZ)
                .collect(Collectors.toSet());
        LOGGER.debug("Chosen private subnets: [{}]", chosenPrivateSubnets);
        return chosenPrivateSubnets;
    }

    private Map<String, Collection<CloudSubnet>> collectPrivateSubnetPerAz(List<CloudSubnet> subnetMetas) {
        return subnetMetas.stream()
                .filter(CloudSubnet::isPrivateSubnet)
                .collect(Multimaps.toMultimap(CloudSubnet::getAvailabilityZone, sm -> sm, ArrayListMultimap::create)).asMap();
    }
}
