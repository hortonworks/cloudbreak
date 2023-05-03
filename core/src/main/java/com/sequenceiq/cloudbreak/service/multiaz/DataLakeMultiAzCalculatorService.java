package com.sequenceiq.cloudbreak.service.multiaz;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.common.api.type.InstanceGroupName;

@Service
public class DataLakeMultiAzCalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataLakeMultiAzCalculatorService.class);

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private MultiAzCalculatorService multiAzCalculatorService;

    public void calculateByRoundRobin(Map<String, String> subnetAzPairs, Stack stack) {
        if (!blueprintUtils.isEnterpriseDatalake(stack)) {
            multiAzCalculatorService.calculateByRoundRobin(subnetAzPairs, stack);
        } else {
            String stackSubnetId = multiAzCalculatorService.getStackSubnetIdIfExists(stack);
            String stackAz = stackSubnetId == null ? null : subnetAzPairs.get(stackSubnetId);
            calculateByRoundRobinTreatingAuxiliaryAndGatewayAsOne(stack, subnetAzPairs, stackSubnetId, stackAz);
        }
    }

    public void calculateByRoundRobinTreatingAuxiliaryAndGatewayAsOne(Stack stack, Map<String, String> subnetAzPairs, String stackSubnetId, String stackAz) {
        LOGGER.info("Setup multiAZ network for {}", stack);
        for (InstanceGroup instanceGroup : multiAzCalculatorService.sortInstanceGroups(stack)) {
            if (!instanceGroup.getGroupName().equals(InstanceGroupName.AUXILIARY.getName())
                    && !instanceGroup.getGroupName().equals(InstanceGroupName.GATEWAY.getName())) {
                LOGGER.info("Calculating multiAZ to {}", instanceGroup.getGroupName());
                multiAzCalculatorService.calculateByRoundRobin(subnetAzPairs, instanceGroup.getInstanceGroupNetwork(),
                        instanceGroup.getNotDeletedAndNotZombieInstanceMetaDataSet());
                multiAzCalculatorService.prepareInstanceMetaDataSubnetAndAvailabilityZoneAndRackId(stackSubnetId, stackAz, instanceGroup);
            }
        }
        Optional<InstanceGroup> auxiliaryHostGroup = getInstanceGroupByInstanceGroupName(stack, InstanceGroupName.AUXILIARY);
        Optional<InstanceGroup> gatewayHostGroup = getInstanceGroupByInstanceGroupName(stack, InstanceGroupName.GATEWAY);
        if (!(auxiliaryHostGroup.isEmpty() && gatewayHostGroup.isEmpty())) {
            Set<InstanceMetaData> mergedInstanceMetaData = new HashSet<>();
            mergedInstanceMetaData.addAll(getInstanceMetadata(stack, InstanceGroupName.GATEWAY));
            mergedInstanceMetaData.addAll(getInstanceMetadata(stack, InstanceGroupName.AUXILIARY));
            AtomicReference<InstanceGroupNetwork> instanceGroupNetwork = new AtomicReference<>(new InstanceGroupNetwork());
            auxiliaryHostGroup.ifPresentOrElse(hostgroup -> instanceGroupNetwork.set(auxiliaryHostGroup.get().getInstanceGroupNetwork()),
                    () -> instanceGroupNetwork.set(gatewayHostGroup.get().getInstanceGroupNetwork()));
            LOGGER.info("Auxiliary/Gateway hostgroup's Metadata: {}", mergedInstanceMetaData);
            LOGGER.info("Calculation being done for Auxiliary/Gateway");
            multiAzCalculatorService.calculateByRoundRobin(subnetAzPairs, instanceGroupNetwork.get(), mergedInstanceMetaData);
            if (auxiliaryHostGroup.isPresent()) {
                multiAzCalculatorService.prepareInstanceMetaDataSubnetAndAvailabilityZoneAndRackId(stackSubnetId, stackAz, auxiliaryHostGroup.get());
            }
            if (gatewayHostGroup.isPresent()) {
                multiAzCalculatorService.prepareInstanceMetaDataSubnetAndAvailabilityZoneAndRackId(stackSubnetId, stackAz, gatewayHostGroup.get());
            }
        }
    }

    private Set<InstanceMetaData> getInstanceMetadata(Stack stack, InstanceGroupName groupName) {
        AtomicReference<Set<InstanceMetaData>> instanceMetaDataSet = new AtomicReference<>(new HashSet<>());
        getInstanceGroupByInstanceGroupName(stack, groupName)
                .ifPresentOrElse(instanceGroup -> instanceMetaDataSet.set(instanceGroup.getNotDeletedAndNotZombieInstanceMetaDataSet()),
                        () -> instanceMetaDataSet.set(Collections.emptySet()));
        return instanceMetaDataSet.get();
    }

    private Optional<InstanceGroup> getInstanceGroupByInstanceGroupName(Stack stack, InstanceGroupName groupName) {
        return stack.getInstanceGroups()
                .stream()
                .filter(instanceGroup -> instanceGroup.getGroupName().equals(groupName.getName()))
                .findFirst();
    }
}
