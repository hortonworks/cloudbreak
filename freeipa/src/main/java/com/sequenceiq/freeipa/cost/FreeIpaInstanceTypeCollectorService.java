package com.sequenceiq.freeipa.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cost.banzai.BanzaiCache;
import com.sequenceiq.cloudbreak.cost.cloudera.ClouderaCostCache;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.StackRepository;

@Service
public class FreeIpaInstanceTypeCollectorService {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private BanzaiCache banzaiCache;

    @Inject
    private ClouderaCostCache clouderaCostCache;

    public ClusterCostDto getAllInstanceTypesByCrn(String crn) {
        Optional<Stack> stackViewDelegate = stackRepository.findOneWithListsByResourceCrn(crn);
        ClusterCostDto clusterCostDto = new ClusterCostDto();
        String region = stackViewDelegate.get().getRegion();
        clusterCostDto.setRegion(region);
        List<InstanceGroupCostDto> instanceGroupCostDtos = new ArrayList<>();
        for (InstanceGroup instanceGroup : stackViewDelegate.get().getInstanceGroups()) {
            String instanceType = instanceGroup.getTemplate().getInstanceType();
            InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
            instanceGroupCostDto.setMemoryPerInstance(banzaiCache.memoryByInstanceType(region, instanceType));
            instanceGroupCostDto.setCoresPerInstance(banzaiCache.cpuByInstanceType(region, instanceType));
            instanceGroupCostDto.setPricePerInstance(banzaiCache.priceByInstanceType(region, instanceType));
            instanceGroupCostDto.setClouderaPricePerInstance(clouderaCostCache.getPriceByType(instanceType));
            instanceGroupCostDto.setType(instanceType);
            instanceGroupCostDto.setCount(instanceGroup.getInstanceMetaData().size());
            instanceGroupCostDtos.add(instanceGroupCostDto);
        }
        clusterCostDto.setInstanceGroups(instanceGroupCostDtos);
        return clusterCostDto;
    }
}
