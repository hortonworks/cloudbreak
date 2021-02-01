package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.repository.TargetGroupRepository;

@Service
public class TargetGroupPersistenceService {

    @Inject
    private TargetGroupRepository repository;

    public TargetGroup save(TargetGroup targetGroup) {
        return repository.save(targetGroup);
    }

    public Iterable<TargetGroup> saveAll(Iterable<TargetGroup> targetGroups) {
        return repository.saveAll(targetGroups);
    }

    public Set<TargetGroup> findByLoadBalancerId(Long loadBalancerId) {
        Set<TargetGroup> targetGroups = repository.findTargetGroupsByLoadBalancerId(loadBalancerId);
        if (targetGroups == null || targetGroups.isEmpty()) {
            targetGroups = repository.findByLoadBalancerId(loadBalancerId);
        }
        return targetGroups;
    }

    public Set<TargetGroup> findByInstanceGroupId(Long instanceGroupId) {
        return repository.findByInstanceGroupId(instanceGroupId);
    }
}
