package com.sequenceiq.cloudbreak.service.multiaz;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_IDS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.cloudbreak.domain.stack.instance.AvailabilityZone;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Service
public class MultiAzCalculatorService {

    private static final String DEFAULT_RACK = "default-rack";

    private static final int NUMBER_OF_GROUPS = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzCalculatorService.class);

    @Inject
    private MultiAzValidator multiAzValidator;

    public String determineRackId(String subnetId, String availabilityZone) {
        return "/" +
                (isNullOrEmpty(availabilityZone) ?
                        (isNullOrEmpty(subnetId) ? DEFAULT_RACK : subnetId)
                        : availabilityZone);
    }

    public Map<String, String> prepareSubnetAzMap(DetailedEnvironmentResponse environment) {
        return prepareSubnetAzMap(environment, null);
    }

    public Map<String, String> prepareSubnetAzMap(DetailedEnvironmentResponse environment, String availabilityZone) {
        if (availabilityZone == null) {
            LOGGER.debug("Collect all AZ in environment");
        } else {
            LOGGER.debug("Filter the AZs by {}", availabilityZone);
        }
        Map<String, String> subnetAzPairs = new HashMap<>();
        if (environment != null && environment.getNetwork() != null && environment.getNetwork().getSubnetMetas() != null) {
            for (Map.Entry<String, CloudSubnet> entry : environment.getNetwork().getSubnetMetas().entrySet()) {
                CloudSubnet value = entry.getValue();
                if (needToAddAZ(value, availabilityZone)) {
                    if (!isNullOrEmpty(value.getId())) {
                        subnetAzPairs.put(value.getId(), value.getAvailabilityZone());
                    } else if (!isNullOrEmpty(value.getName())) {
                        subnetAzPairs.put(value.getName(), value.getAvailabilityZone());
                    }
                }
            }
        }
        return subnetAzPairs;
    }

    private boolean needToAddAZ(CloudSubnet value, String availabilityZone) {
        return availabilityZone == null || availabilityZone.equals(value.getAvailabilityZone());
    }

    public void calculateByRoundRobin(Map<String, String> subnetAzPairs, InstanceGroup instanceGroup) {
        LOGGER.trace("Calculate the subnet by round robin from {}", subnetAzPairs);
        Map<String, Integer> subnetUsage = new HashMap<>();
        Set<String> subnetIds = collectSubnetIds(instanceGroup, NetworkScaleDetails.getEmpty());
        LOGGER.trace("Collected subnetIds: {}", subnetIds);
        initializeSubnetUsage(subnetAzPairs, subnetIds, subnetUsage);
        List<InstanceMetadataView> instanceMetadataViews = new ArrayList<>(instanceGroup.getNotDeletedAndNotZombieInstanceMetaDataSet());
        collectCurrentSubnetUsage(instanceMetadataViews, subnetUsage);

        if (!subnetIds.isEmpty() && multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)) {
            checkSubnetUsageCount(subnetUsage, subnetIds);
            for (InstanceMetaData instanceMetaData : instanceGroup.getNotDeletedAndNotZombieInstanceMetaDataSet()) {
                if (isNullOrEmpty(instanceMetaData.getSubnetId())) {
                    Integer numberOfInstanceInASubnet = searchTheSmallestInstanceCountForUsage(subnetUsage);
                    String leastUsedSubnetId = searchTheSmallestUsedID(subnetUsage, numberOfInstanceInASubnet);

                    instanceMetaData.setSubnetId(leastUsedSubnetId);
                    instanceMetaData.setAvailabilityZone(subnetAzPairs.get(leastUsedSubnetId));

                    subnetUsage.put(leastUsedSubnetId, numberOfInstanceInASubnet + 1);
                }
            }
        }
    }

    public void calculateByRoundRobin(Map<String, String> subnetAzPairs, InstanceGroupDto instanceGroupDto, InstanceMetaData instanceMetaData,
            NetworkScaleDetails networkScaleDetails) {
        LOGGER.debug("Calculate the subnet by round robin for {} from {}", instanceMetaData.getDiscoveryFQDN(), subnetAzPairs);
        Map<String, Integer> subnetUsage = new HashMap<>();
        InstanceGroupView instanceGroup = instanceGroupDto.getInstanceGroup();
        Set<String> subnetIds = collectSubnetIds(instanceGroup, networkScaleDetails);
        LOGGER.debug("Collected subnetIds: {}", subnetIds);
        initializeSubnetUsage(subnetAzPairs, subnetIds, subnetUsage);
        collectCurrentSubnetUsage(instanceGroupDto.getNotDeletedAndNotZombieInstanceMetaData(), subnetUsage);
        if (!subnetIds.isEmpty() && multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)) {
            checkSubnetUsageCount(subnetUsage, subnetIds);
            if (isNullOrEmpty(instanceMetaData.getSubnetId())) {
                Integer numberOfInstanceInASubnet = searchTheSmallestInstanceCountForUsage(subnetUsage);
                String leastUsedSubnetId = searchTheSmallestUsedID(subnetUsage, numberOfInstanceInASubnet);
                LOGGER.debug("Smallest count is {} with subnet: {}", numberOfInstanceInASubnet, leastUsedSubnetId);

                instanceMetaData.setSubnetId(leastUsedSubnetId);
                instanceMetaData.setAvailabilityZone(subnetAzPairs.get(leastUsedSubnetId));
                subnetUsage.put(leastUsedSubnetId, numberOfInstanceInASubnet + 1);
            }
        }
    }

    private void checkSubnetUsageCount(Map<String, Integer> subnetUsage, Set<String> subnetIds) {
        if (subnetUsage.isEmpty()) {
            LOGGER.debug("The following subnets are missing from the Environment, you may have removed them during an environment update previously? " +
                    "Missing subnets: {}", subnetIds);
            throw new CloudbreakServiceException("The following subnets are missing from the Environment, you may have removed them during an environment " +
                    "update previously? Missing subnets: " + subnetIds);
        }
        LOGGER.debug("Subnet usage: {}", subnetUsage);
    }

    public Map<String, String> filterSubnetByLeastUsedAz(InstanceGroupDto instanceGroup, Map<String, String> subnetAzPairs) {
        LOGGER.debug("Filter the subnet and az pairs with the least used AZ. {}", subnetAzPairs);
        Map<String, List<String>> azSubnetPairs = createAzSubnetPairsFromSubnetAzPairs(subnetAzPairs);
        LOGGER.debug("Converted az and subnet pairs: {}", azSubnetPairs);
        Map<String, Integer> azUsage = new HashMap<>();
        Set<String> azs = azSubnetPairs.keySet();
        azs.forEach(az -> azUsage.computeIfAbsent(az, k -> 0));
        collectCurrentAzUsage(instanceGroup, azUsage, subnetAzPairs);
        LOGGER.debug("AZ usage: {}", azUsage);
        Integer numberOfInstanceInAnAz = searchTheSmallestInstanceCountForUsage(azUsage);
        String leastUsedAz = searchTheSmallestUsedID(azUsage, numberOfInstanceInAnAz);
        Set<String> subnetsForLeastUsedAz = new HashSet<>(azSubnetPairs.get(leastUsedAz));
        return subnetsForLeastUsedAz.stream().collect(Collectors.toMap(Function.identity(), v -> leastUsedAz));
    }

    private Map<String, List<String>> createAzSubnetPairsFromSubnetAzPairs(Map<String, String> subnetAzPairs) {
        Map<String, List<String>> ret = new HashMap<>();
        subnetAzPairs.forEach((subnet, az) -> {
            List<String> subnetIds = ret.computeIfAbsent(az, key -> new ArrayList<>());
            subnetIds.add(subnet);
        });
        return ret;
    }

    private void collectCurrentAzUsage(InstanceGroupDto instanceGroup, Map<String, Integer> azUsage, Map<String, String> subnetAzPairs) {
        for (InstanceMetadataView instanceMetaData : instanceGroup.getNotDeletedAndNotZombieInstanceMetaData()) {
            String subnetId = instanceMetaData.getSubnetId();
            if (!isNullOrEmpty(subnetId)) {
                String az = subnetAzPairs.get(subnetId);
                Integer countOfInstances = azUsage.get(az);
                if (countOfInstances != null) {
                    azUsage.put(az, countOfInstances + 1);
                } else {
                    LOGGER.warn("AZ with subnet ID {} is not present in the environment networks. Current usage: {}",
                            subnetId, azUsage.keySet());
                }
            }
        }
    }

    private Integer searchTheSmallestInstanceCountForUsage(Map<String, Integer> usage) {
        return Collections.min(usage.values());
    }

    private String searchTheSmallestUsedID(Map<String, Integer> usage, Integer currentNumber) {
        return usage.entrySet()
                .stream()
                .filter(e -> e.getValue().equals(currentNumber))
                .findFirst()
                .get()
                .getKey();
    }

    private void initializeSubnetUsage(Map<String, String> subnetAzPairs, Set<String> subnetIds, Map<String, Integer> subnetUsage) {
        for (String subnetId : subnetAzPairs.keySet()) {
            if (subnetIds.contains(subnetId)) {
                subnetUsage.put(subnetId, 0);
            }
        }
    }

    private void collectCurrentSubnetUsage(List<InstanceMetadataView> instanceMetadataViews, Map<String, Integer> subnetUsage) {

        for (InstanceMetadataView instanceMetaData : instanceMetadataViews) {
            String subnetId = instanceMetaData.getSubnetId();
            if (!isNullOrEmpty(subnetId)) {
                Integer countOfInstances = subnetUsage.get(subnetId);
                if (countOfInstances != null) {
                    subnetUsage.put(subnetId, countOfInstances + 1);
                } else {
                    LOGGER.warn("Subnet ID {} is not present in the environment networks. Current usage: {}",
                            subnetId, subnetUsage.keySet());
                }
            }
        }
    }

    private Set<String> collectSubnetIds(InstanceGroupView instanceGroup, NetworkScaleDetails networkScaleDetails) {
        Set<String> allSubnetIds = new HashSet<>();
        InstanceGroupNetwork instanceGroupNetwork = instanceGroup.getInstanceGroupNetwork();
        if (instanceGroupNetwork != null) {
            Json attributes = instanceGroupNetwork.getAttributes();
            if (attributes != null) {
                List<String> subnetIds = (List<String>) attributes
                        .getMap()
                        .getOrDefault(SUBNET_IDS, new ArrayList<>());
                if (isPreferredSubnetsSpecifiedForScaling(networkScaleDetails)) {
                    List<String> preferredSubnetIds = networkScaleDetails.getPreferredSubnetIds();
                    String preferredSubnetIdList = String.join(",", preferredSubnetIds);
                    LOGGER.debug("Collect subnet ids considering the customer preferred subnet ids: {}", preferredSubnetIdList);
                    Set<String> subnetIdsMatchedWithPreferred = subnetIds.stream()
                            .filter(preferredSubnetIds::contains)
                            .collect(Collectors.toSet());
                    LOGGER.info("Filtered subnet id list '{}' based on the customer preferred subnet id list '{}'",
                            String.join(",", subnetIdsMatchedWithPreferred), preferredSubnetIdList);
                    allSubnetIds.addAll(subnetIdsMatchedWithPreferred);
                } else {
                    allSubnetIds.addAll(subnetIds);
                }
            }

        }
        return allSubnetIds;
    }

    private boolean isPreferredSubnetsSpecifiedForScaling(NetworkScaleDetails networkScaleDetails) {
        return networkScaleDetails != null
                && CollectionUtils.isNotEmpty(networkScaleDetails.getPreferredSubnetIds());
    }

    public void calculate(Map<String, String> subnetAzPairs, InstanceGroup instanceGroup, Set<GroupPlacement> groupPlacementList,
            SdxClusterShape sdxClusterShape) {
        if (isGroupMultiAZ(sdxClusterShape, groupPlacementList)) {
            calculateByGroup(instanceGroup, groupPlacementList);
        } else {
            calculateByRoundRobin(subnetAzPairs, instanceGroup);
        }
    }

    private boolean isGroupMultiAZ(SdxClusterShape sdxClusterShape, Set<GroupPlacement> groupPlacementList) {
        return !groupPlacementList.isEmpty() &&
                (sdxClusterShape == SdxClusterShape.SCALABLE || sdxClusterShape == SdxClusterShape.MEDIUM_DUTY_HA);
    }

    private void calculateByGroup(InstanceGroup instanceGroup, Set<GroupPlacement> groupPlacementList) {
        LOGGER.trace("?????????????????????? Calculate the subnet by group");
        List<InstanceMetadataView> instanceMetadataViews = new ArrayList<>(instanceGroup.getNotDeletedAndNotZombieInstanceMetaDataSet());
        groupPlacementList.stream().forEach(groupPlacement -> collectCurrentSubnetUsage(instanceMetadataViews, groupPlacement.getSubnetUsage()));

        if (multiAzValidator.supportedForInstanceMetadataGeneration(instanceGroup)) {
            List<String> subnetIds = new ArrayList<>();
            Set<String> availabilityZones = new HashSet<>();
            for (InstanceMetaData instanceMetaData : instanceGroup.getNotDeletedAndNotZombieInstanceMetaDataSet()) {
                LOGGER.info("################## XXXXXXXXXXXXXXXX #####################");
                if (isNullOrEmpty(instanceMetaData.getSubnetId())) {
                    LOGGER.info("################## YYYYYYYYYYYYYYYYYYY #####################");

                    GroupPlacement groupPlacement = getGroupPlacementForComponent(instanceMetaData.getInstanceGroupName(),
                            instanceMetaData.getInstanceGroup().getHints(), groupPlacementList);
                    Integer numberOfInstanceInASubnet = searchTheSmallestInstanceCountForUsage(groupPlacement.getSubnetUsage());
                    String leastUsedSubnetId = searchTheSmallestUsedID(groupPlacement.getSubnetUsage(), numberOfInstanceInASubnet);

                    instanceMetaData.setSubnetId(leastUsedSubnetId);
                    instanceMetaData.setAvailabilityZone(groupPlacement.getAvailabilityZone());
                    groupPlacement.addComponent(instanceMetaData.getInstanceGroupName());
                    groupPlacement.increaseSubnetUsage(leastUsedSubnetId);
                    subnetIds.add(leastUsedSubnetId);
                    availabilityZones.add(groupPlacement.getAvailabilityZone());
                }
            }
            Map<String, Object> map = instanceGroup.getInstanceGroupNetwork().getAttributes().getMap();
            map.put(SUBNET_IDS, subnetIds);
            instanceGroup.getInstanceGroupNetwork().setAttributes(new Json(map));
            Set<AvailabilityZone> azs = new HashSet<>();
            availabilityZones.stream().forEach(azS -> {
                AvailabilityZone az = new AvailabilityZone();
                az.setAvailabilityZone(azS);
                az.setInstanceGroup(instanceGroup);
                azs.add(az);

            });
            instanceGroup.setAvailabilityZones(azs);
            LOGGER.info(instanceGroup.getInstanceGroupNetwork().getAttributes().toString());
        }
    }

    private GroupPlacement getGroupPlacementForComponent(String componentName, Set<String> hints, Set<GroupPlacement> groupPlacementList) {
        return groupPlacementList
                .stream()
                .filter(gp -> hints.contains(gp.getName()))
                .filter(gp -> !gp.containsComponent(componentName))
                .findFirst()
                .orElseThrow(() -> new CloudbreakServiceException(String.format("Node placement not found for %s", componentName)));
    }

    public Set<GroupPlacement> prepareGroupAzMap(Map<String, String> subnetAzPairs, Set<InstanceGroup> instanceGroups) {
        Set<String> groups = getGroups(instanceGroups);
        Set<String> availabilityZones = getAvailabilityZones(subnetAzPairs);

        if (availabilityZones.size() < NUMBER_OF_GROUPS || groups.size() != NUMBER_OF_GROUPS) {
            return Collections.emptySet();
        }

        Set<GroupPlacement> groupPlacements = new HashSet<>();
        Iterator<String> availabilityZonesIt = availabilityZones.iterator();
        groups.forEach(gp -> {
            String az = availabilityZonesIt.next();
            GroupPlacement groupPlacement = new GroupPlacement(gp, az);
            groupPlacements.add(groupPlacement);

            subnetAzPairs
                    .entrySet()
                    .stream()
                    .filter(entry -> entry.getValue().equals(az))
                    .map(Map.Entry::getKey)
                    .forEach(subnet -> groupPlacement.addSubnet(subnet));
        });
        return groupPlacements;
    }

    private Set<String> getAvailabilityZones(Map<String, String> subnetAzPairs) {
        return subnetAzPairs
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    private Set<String> getGroups(Set<InstanceGroup> instanceGroups) {
        return instanceGroups
                .stream()
                .filter(ig -> Objects.nonNull(ig.getHints()))
                .map(InstanceGroup::getHints)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    public static class GroupPlacement {
        private final String name;

        private final String availabilityZone;

        private final Set<String> components;

        private final Map<String, Integer> subnetUsage;

        public GroupPlacement(String name, String availabilityZone) {
            this.name = name;
            this.availabilityZone = availabilityZone;
            this.components = new HashSet<>();
            this.subnetUsage = new HashMap<>();
        }

        public String getName() {
            return name;
        }

        public String getAvailabilityZone() {
            return availabilityZone;
        }

        public boolean containsComponent(String componentName) {
            return components.contains(componentName);
        }

        public void addComponent(String componentName) {
            components.add(componentName);
        }

        public void addSubnet(String subnet) {
            subnetUsage.put(subnet, 0);
        }

        public void increaseSubnetUsage(String subnet) {
            subnetUsage.put(subnet, subnetUsage.getOrDefault(subnet, 0) + 1);
        }

        public Map<String, Integer> getSubnetUsage() {
            return subnetUsage;
        }
    }
}
