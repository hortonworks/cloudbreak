package com.sequenceiq.cloudbreak.service.cost;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;

@Service
public class InstanceTypeCollectorService {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private InstanceGroupService instanceGroupService;

    public Map<String, Long> getAllInstanceTypesByCrn(String crn) {
        Stack stack = stackRepository.findByResourceCrn(crn).get();
        //get list of all instancetype
        List<InstanceGroupView> instanceGroupList = instanceGroupService.getInstanceGroupViewByStackId(stack.getId());
        Map<String, Long> countByInstanceType = instanceGroupList.stream()
                .collect(Collectors.groupingBy(ig -> ig.getTemplate().getInstanceType(), Collectors.counting()));
        return countByInstanceType;
    }
}
