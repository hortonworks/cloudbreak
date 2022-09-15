package com.sequenceiq.cloudbreak.service.cost;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cost.banzai.BanzaiCache;
import com.sequenceiq.cloudbreak.cost.cloudera.ClouderaCostCache;
import com.sequenceiq.cloudbreak.cost.model.DiskCostDto;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.repository.StackDtoRepository;
import com.sequenceiq.cloudbreak.cost.model.ClusterCostDto;
import com.sequenceiq.cloudbreak.cost.model.InstanceGroupCostDto;
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

    @Inject
    private ClouderaCostCache clouderaCostCache;

    public ClusterCostDto getAllInstanceTypesByCrn(String crn) {
        Optional<StackViewDelegate> stackViewDelegate = stackRepository.findByCrn(crn);
        //get list of all instancetype
        List<InstanceGroupView> instanceGroupList = instanceGroupService.getInstanceGroupViewByStackId(stackViewDelegate.get().getId());
        ClusterCostDto clusterCostDto = new ClusterCostDto();
        clusterCostDto.setStatus(stackViewDelegate.get().getStackStatus().getStatus().name());
        String region = stackViewDelegate.get().getRegion();
        clusterCostDto.setRegion(region);
        List<InstanceGroupCostDto> instanceGroupCostDtos = new ArrayList<>();
        for (InstanceGroupView instanceGroupView : instanceGroupList) {
            int count = instanceMetaDataService.countByInstanceGroupId(instanceGroupView.getId());
            String instanceType = instanceGroupView.getTemplate().getInstanceType();
            InstanceGroupCostDto instanceGroupCostDto = new InstanceGroupCostDto();
            instanceGroupCostDto.setMemoryPerInstance(banzaiCache.memoryByInstanceType(region, instanceType));
            instanceGroupCostDto.setCoresPerInstance(banzaiCache.cpuByInstanceType(region, instanceType));
            instanceGroupCostDto.setPricePerInstance(banzaiCache.priceByInstanceType(region, instanceType));
            instanceGroupCostDto.setClouderaPricePerInstance(clouderaCostCache.getPriceByType(instanceType));
            instanceGroupCostDto.setType(instanceType);
            instanceGroupCostDto.setCount(count);

            List<DiskCostDto> diskCostDtos = new ArrayList<>();
            for (VolumeTemplate volumeTemplate : instanceGroupView.getTemplate().getVolumeTemplates()) {
                DiskCostDto diskCostDto = new DiskCostDto();
                diskCostDto.setCount(volumeTemplate.getVolumeCount());
                diskCostDto.setSize(volumeTemplate.getVolumeSize());
                diskCostDto.setPricePerDiskGB(0.000138);
                diskCostDtos.add(diskCostDto);
            }
            instanceGroupCostDto.setDisksPerInstance(diskCostDtos);

            instanceGroupCostDtos.add(instanceGroupCostDto);
        }
        clusterCostDto.setInstanceGroups(instanceGroupCostDtos);
        return clusterCostDto;
    }
}
