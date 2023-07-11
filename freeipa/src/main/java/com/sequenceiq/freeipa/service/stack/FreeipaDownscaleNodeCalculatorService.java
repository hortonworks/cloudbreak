package com.sequenceiq.freeipa.service.stack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimaps;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityInfo;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleRequest;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class FreeipaDownscaleNodeCalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaDownscaleNodeCalculatorService.class);

    private static final Predicate<InstanceMetaData> NON_PRIMARY_GATEWAY_PREDICATE = imd ->
            imd.getInstanceMetadataType() != InstanceMetadataType.GATEWAY_PRIMARY;

    public ArrayList<String> calculateDownscaleCandidates(Stack stack, AvailabilityInfo originalAvailabilityInfo, AvailabilityType targetAvailabilityType,
            Set<String> instanceIdsToDelete) {
        ArrayList<String> downscaleCandidates = CollectionUtils.isEmpty(instanceIdsToDelete)
                ? calculateDownscaleCandidates(stack, originalAvailabilityInfo, targetAvailabilityType)
                : new ArrayList<>(instanceIdsToDelete);
        LOGGER.debug("Freeipa downscale candidates are: {}", downscaleCandidates);
        return downscaleCandidates;
    }

    private ArrayList<String> calculateDownscaleCandidates(Stack stack, AvailabilityInfo originalAvailabilityInfo, AvailabilityType targetAvailabilityType) {
        int instancesToRemove = originalAvailabilityInfo.getActualNodeCount() - targetAvailabilityType.getInstanceCount();
        Set<InstanceMetaData> notDeletedInstanceMetadataSet = stack.getNotDeletedInstanceMetaDataSet();
        LOGGER.debug("Calculating nodes to remove during freeipa downscale. Number of nodes to remove: {}, not deleted instanceMetaData: {}",
                instancesToRemove, notDeletedInstanceMetadataSet);
        if (stack.isMultiAz()) {
            LOGGER.debug("{} is Multi Az so taking Availability Zones into account for selecting the downscale candidates", stack.getName());
            return new ArrayList<>(calculateDownscaleCandidatesForMultiAz(notDeletedInstanceMetadataSet, instancesToRemove));
        } else {
            return notDeletedInstanceMetadataSet.stream()
                    .filter(NON_PRIMARY_GATEWAY_PREDICATE)
                    .limit(instancesToRemove)
                    .map(InstanceMetaData::getInstanceId)
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private List<String> calculateDownscaleCandidatesForMultiAz(Set<InstanceMetaData> allInstances, int numInstancesToRemove) {
        Map<String, Collection<InstanceMetaData>> availabilityZoneToNodesMap = allInstances.stream().collect(Multimaps.toMultimap(
                InstanceMetaData::getAvailabilityZone,
                Function.identity(),
                ArrayListMultimap::create)).asMap();
        LOGGER.debug("Availability Zone to Nodes mapping is {}", availabilityZoneToNodesMap);
        List<String> instancesToRemove = new ArrayList<>();
        for (int instanceCountToRemove = numInstancesToRemove; !availabilityZoneToNodesMap.isEmpty() && instanceCountToRemove > 0; instanceCountToRemove--) {
            Map.Entry<String, Collection<InstanceMetaData>> availabilityZoneWithInstances = Collections.max(availabilityZoneToNodesMap.entrySet(),
                    Map.Entry.comparingByValue(comparatorForDownscaleNodesInMultiAz()));
            LOGGER.debug("Node for downscale is present in {}", availabilityZoneWithInstances.getKey());
            Optional<InstanceMetaData> instanceToDelete = availabilityZoneWithInstances.getValue().stream()
                    .filter(NON_PRIMARY_GATEWAY_PREDICATE)
                    .findFirst();
            instanceToDelete.ifPresent(instance -> {
                instancesToRemove.add(instance.getInstanceId());
                availabilityZoneWithInstances.getValue().remove(instance);
            });
        }
        LOGGER.debug("Downscale candidates for multiAz stack are {}", instancesToRemove);
        return instancesToRemove;
    }

    public AvailabilityType calculateTargetAvailabilityType(DownscaleRequest request, int currentNodeCount) {
        AvailabilityType calculatedAvailabilityType = request.getTargetAvailabilityType() != null
                ? request.getTargetAvailabilityType()
                : AvailabilityType.getByInstanceCount(currentNodeCount - request.getInstanceIds().size());
        LOGGER.debug("Calculated target availability type for freeipa downscale: {}", calculatedAvailabilityType);
        return calculatedAvailabilityType;
    }

    private Comparator<Collection<InstanceMetaData>> comparatorForDownscaleNodesInMultiAz() {
        return (instances1, instances2) -> {
            if (instances1.size() == instances2.size()) {
                return (int) (countNonPrimaryGateways(instances1) - countNonPrimaryGateways(instances2));
            } else {
                return instances1.size() - instances2.size();
            }
        };
    }

    private long countNonPrimaryGateways(Collection<InstanceMetaData> instances) {
        return instances.stream().filter(NON_PRIMARY_GATEWAY_PREDICATE).count();
    }

}