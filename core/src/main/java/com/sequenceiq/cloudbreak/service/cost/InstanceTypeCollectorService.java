package com.sequenceiq.cloudbreak.service.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.banzai.BanzaiCache;
import com.sequenceiq.cloudbreak.repository.StackDtoRepository;
import com.sequenceiq.cloudbreak.service.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.service.cost.model.InstanceGroupCostDto;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.delegate.StackViewDelegate;

@Service
public class InstanceTypeCollectorService {

    @Inject
    private StackDtoRepository stackRepository;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private BanzaiCache banzaiCache;

    public ClusterCostDto getAllInstanceTypesByCrn(String crn) {
        Optional<StackViewDelegate> stackViewDelegate = stackRepository.findByCrn(crn);
        //get list of all instancetype
        List<InstanceGroupView> instanceGroupList = instanceGroupService.getInstanceGroupViewByStackId(stackViewDelegate.get().getId());
        ClusterCostDto clusterCostDto = new ClusterCostDto();
        String region = stackViewDelegate.get().getRegion();
        clusterCostDto.setRegion(region);
        List<InstanceGroupCostDto> instanceGroupCostDtos = new ArrayList<>();
        for (InstanceGroupView instanceGroupView : instanceGroupList) {
            int count = instanceMetaDataService.countByInstanceGroupId(instanceGroupView.getId());
            String instanceType = instanceGroupView.getTemplate().getInstanceType();
            InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
            instanceGroupCostDto.setMemoryPerInstance(banzaiCache.memoryByInstanceType(region, instanceType));
            instanceGroupCostDto.setCoresPerInstance(banzaiCache.cpuByInstanceType(region, instanceType));
            instanceGroupCostDto.setType(instanceType);
            instanceGroupCostDto.setCount(count);
        }
        clusterCostDto.setInstanceGroups(instanceGroupCostDtos);
        return clusterCostDto;
    }
}
