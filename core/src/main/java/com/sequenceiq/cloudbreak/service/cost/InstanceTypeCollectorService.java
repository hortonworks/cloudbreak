package com.sequenceiq.cloudbreak.service.cost;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.StackDtoRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.StackViewRepository;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.view.delegate.StackViewDelegate;

@Service
public class InstanceTypeCollectorService {

    @Inject
    private StackDtoRepository stackRepository;

    @Inject
    private InstanceGroupService instanceGroupService;

    public Map<String, Long> getAllInstanceTypesByCrn(String crn) {
        Optional<StackViewDelegate> stackViewDelegate = stackRepository.findByCrn(crn);
        //get list of all instancetype
        List<InstanceGroupView> instanceGroupList = instanceGroupService.getInstanceGroupViewByStackId(stackViewDelegate.get().getId());
        Map<String, Long> countByInstanceType = instanceGroupList.stream()
                .collect(Collectors.groupingBy(ig -> ig.getTemplate().getInstanceType(), Collectors.counting()));
        return countByInstanceType;
    }
}
