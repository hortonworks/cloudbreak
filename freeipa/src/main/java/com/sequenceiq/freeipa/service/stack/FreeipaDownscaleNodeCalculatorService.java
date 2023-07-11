package com.sequenceiq.freeipa.service.stack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityInfo;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.AvailabilityType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.scale.DownscaleRequest;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class FreeipaDownscaleNodeCalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaDownscaleNodeCalculatorService.class);

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
        Set<InstanceMetaData> nonPrimaryGatewayInstances = notDeletedInstanceMetadataSet.stream()
                .filter(imd -> imd.getInstanceMetadataType() != InstanceMetadataType.GATEWAY_PRIMARY).collect(Collectors.toSet());
        if (stack.isMultiAz()) {
            return new ArrayList<>(calculateDownscaleCandidatesForMultiAz(nonPrimaryGatewayInstances, instancesToRemove));
        } else {
            return nonPrimaryGatewayInstances.stream().limit(instancesToRemove)
                    .map(InstanceMetaData::getInstanceId)
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    private List<String> calculateDownscaleCandidatesForMultiAz(Set<InstanceMetaData> instances, int numInstancesToRemove) {
        Map<String, List<String>> availabilityZoneToNodesMap = instances.stream().collect(Collectors.toMap(instance -> instance.getAvailabilityZone(),
                instance -> Stream.of(instance.getInstanceId()).collect(Collectors.toList()), (first, second) -> {
            first.addAll(second);
            return first;
            }));
        List<String> instancesToRemove = new ArrayList<>();
        while (!availabilityZoneToNodesMap.isEmpty() && numInstancesToRemove-- > 0) {
            String availabilityZone = Collections.max(availabilityZoneToNodesMap.entrySet(),
                    Map.Entry.comparingByValue(Comparator.comparingInt(List::size))).getKey();
            List<String> selectedInstances = availabilityZoneToNodesMap.get(availabilityZone);
            instancesToRemove.add(selectedInstances.remove(selectedInstances.size() - 1));
            if (CollectionUtils.isEmpty(selectedInstances)) {
                availabilityZoneToNodesMap.remove(availabilityZone);
            }
        }
        return instancesToRemove;
    }

    public AvailabilityType calculateTargetAvailabilityType(DownscaleRequest request, int currentNodeCount) {
        AvailabilityType calculatedAvailabilityType = request.getTargetAvailabilityType() != null
                ? request.getTargetAvailabilityType()
                : AvailabilityType.getByInstanceCount(currentNodeCount - request.getInstanceIds().size());
        LOGGER.debug("Calculated target availability type for freeipa downscale: {}", calculatedAvailabilityType);
        return calculatedAvailabilityType;
    }

}