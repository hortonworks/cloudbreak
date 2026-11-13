package com.sequenceiq.cloudbreak.service.multiaz;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.common.api.type.InstanceGroupName;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class DataLakeAwareInstanceMetadataAvailabilityZoneCalculator extends InstanceMetadataAvailabilityZoneCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataLakeAwareInstanceMetadataAvailabilityZoneCalculator.class);

    private static final String DEFAULT_RACK = "default-rack";

    @Inject
    private BlueprintUtils blueprintUtils;

    @Override
    public void populate(Long stackId) {
        Stack stack = getStackService().getByIdWithLists(stackId);
        if (populateSupportedOnStack(stack)) {
            if (blueprintUtils.isEnterpriseDatalake(stack)) {
                populateForEnterpriseDataLake(stack);
            } else {
                LOGGER.info("Populate availability zones with round-robin fashion for every instance group.");
                populate(stack);
            }
        } else {
            LOGGER.debug("Stack is NOT multi-AZ enabled or AvailabilityZone connector doesn't exist for platform, populating zones and subnets for instances");
            String stackSubnetId = getStackSubnetIdIfExists(stack);
            DetailedEnvironmentResponse environment = getDetailedEnvironmentResponse(stack.getEnvironmentCrn());
            Map<String, String> subnetAzMap = prepareSubnetAzMap(environment);
            String stackAz = stackSubnetId == null ? null : subnetAzMap.get(stackSubnetId);
            stack.getInstanceGroups().forEach(ig -> prepareInstanceMetaDataSubnetAndAvailabilityZoneAndRackId(stackSubnetId, stackAz, ig));
            Set<InstanceMetaData> instancesToBeUpdated = new HashSet<>();
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                instancesToBeUpdated.addAll(instanceGroup.getInstanceMetaData());
            }
            updateInstancesMetaData(instancesToBeUpdated);
        }
    }

    private Map<String, String> prepareSubnetAzMap(DetailedEnvironmentResponse environment) {
        Map<String, String> subnetAzPairs = new HashMap<>();
        if (environment != null && environment.getNetwork() != null && environment.getNetwork().getSubnetMetas() != null) {
            for (Map.Entry<String, CloudSubnet> entry : environment.getNetwork().getSubnetMetas().entrySet()) {
                CloudSubnet value = entry.getValue();
                if (!isNullOrEmpty(value.getName())) {
                    subnetAzPairs.put(value.getName(), value.getAvailabilityZone());
                    if (!isNullOrEmpty(value.getName())) {
                        subnetAzPairs.put(value.getName(), value.getAvailabilityZone());
                    }
                    if (!isNullOrEmpty(value.getId())) {
                        subnetAzPairs.put(value.getId(), value.getAvailabilityZone());
                    }
                }
            }
        }

        return subnetAzPairs;
    }

    protected void prepareInstanceMetaDataSubnetAndAvailabilityZoneAndRackId(String stackSubnetId, String stackAz, InstanceGroup instanceGroup) {
        for (InstanceMetaData instanceMetaData : instanceGroup.getAllInstanceMetaData()) {
            if (isNullOrEmpty(instanceMetaData.getSubnetId()) && isNullOrEmpty(instanceMetaData.getAvailabilityZone())) {
                instanceMetaData.setSubnetId(stackSubnetId);
                instanceMetaData.setAvailabilityZone(stackAz);
            }
            instanceMetaData.setRackId(determineRackId(instanceMetaData));
        }
    }

    private String determineRackId(InstanceMetaData instanceMetaData) {
        return "/" +
                (isNullOrEmpty(instanceMetaData.getAvailabilityZone()) ?
                        (isNullOrEmpty(instanceMetaData.getSubnetId()) ? DEFAULT_RACK : instanceMetaData.getSubnetId())
                        : instanceMetaData.getAvailabilityZone());
    }

    private void populateForEnterpriseDataLake(Stack stack) {
        LOGGER.info("Populating availability zones for Enterprise Data Lake");
        Set<InstanceMetaData> updatedInstancesMetaData = new HashSet<>();
        String stackSubnetId = getStackSubnetIdIfExists(stack);
        InstanceGroupName pairedGroupName = getQuorumGroupNamePairedWithAuxiliary(stack);
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (notPairedAndNotAuxiliaryGroup(instanceGroup, pairedGroupName)) {
                updatedInstancesMetaData.addAll(populateAvailabilityZonesOnGroup(instanceGroup, stackSubnetId, stack.getNetwork()));
            }
        }
        Optional<InstanceGroup> pairedInstanceGroup = getInstanceGroupByInstanceGroupName(stack, pairedGroupName);
        Optional<InstanceGroup> auxiliaryInstanceGroup = getInstanceGroupByInstanceGroupName(stack, InstanceGroupName.AUXILIARY);
        if (pairedInstanceGroup.isPresent() || auxiliaryInstanceGroup.isPresent()) {
            Set<InstanceMetaData> mergedInstanceMetaData = new HashSet<>();
            pairedInstanceGroup.ifPresent(ig -> mergedInstanceMetaData.addAll(ig.getNotTerminatedInstanceMetaDataSet()));
            auxiliaryInstanceGroup.ifPresent(ig -> mergedInstanceMetaData.addAll(ig.getNotTerminatedInstanceMetaDataSet()));
            LOGGER.info("Auxiliary/{} instance group's meta data: {}", pairedGroupName.getName(), mergedInstanceMetaData);
            Set<String> availabilityZones = pairedInstanceGroup.or(() -> auxiliaryInstanceGroup).get().getAvailabilityZones();
            updatedInstancesMetaData.addAll(
                    populateAvailabilityZoneOfInstances(
                            availabilityZones,
                            mergedInstanceMetaData,
                            pairedInstanceGroup.orElseGet(auxiliaryInstanceGroup::get),
                            stackSubnetId,
                            stack.getNetwork()
                    )
            );
            updateInstancesMetaData(updatedInstancesMetaData);
        } else {
            LOGGER.info("{} and {} instance groups are not present, nothing to do", pairedGroupName.getName(),
                    InstanceGroupName.AUXILIARY.getName());
        }
    }

    private InstanceGroupName getQuorumGroupNamePairedWithAuxiliary(Stack stack) {
        return getInstanceGroupByInstanceGroupName(stack, InstanceGroupName.MASTER).isPresent()
                ? InstanceGroupName.MASTER
                : InstanceGroupName.GATEWAY;
    }

    private static boolean notPairedAndNotAuxiliaryGroup(InstanceGroup instanceGroup, InstanceGroupName pairedGroupName) {
        String instanceGroupName = instanceGroup.getGroupName();
        return !InstanceGroupName.AUXILIARY.getName().equals(instanceGroupName)
                && !pairedGroupName.getName().equals(instanceGroupName);
    }

    private Optional<InstanceGroup> getInstanceGroupByInstanceGroupName(Stack stack, InstanceGroupName groupName) {
        return stack.getInstanceGroups()
                .stream()
                .filter(instanceGroup -> instanceGroup.getGroupName().equals(groupName.getName()))
                .findFirst();
    }
}