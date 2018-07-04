package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;

@Service
public class InstanceGroupService {

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    public Set<InstanceGroup> findByStackId(Long stackId) {
        return instanceGroupRepository.findByStackId(stackId);
    }
}
