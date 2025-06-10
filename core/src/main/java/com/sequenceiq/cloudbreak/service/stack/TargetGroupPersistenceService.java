package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;

import jakarta.inject.Inject;

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
        return repository.findTargetGroupsByLoadBalancerId(loadBalancerId);
    }

    public void delete(Long targetGroupId) {
        repository.deleteById(targetGroupId);
    }
}
