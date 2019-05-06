package com.sequenceiq.freeipa.service.stack;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.repository.InstanceGroupRepository;

@Service
public class InstanceGroupService {

    @Inject
    private InstanceGroupRepository repository;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public Set<InstanceGroup> findByStackId(Long stackId) {
        return repository.findByStackId(stackId);
    }

    public Optional<InstanceGroup> findOneByGroupNameInStack(Long stackId, String groupName) {
        return repository.findOneByGroupNameInStack(stackId, groupName);
    }

    public InstanceGroup save(InstanceGroup instanceGroup) {
        return repository.save(instanceGroup);
    }

    public Set<InstanceGroup> findBySecurityGroup(SecurityGroup securityGroup) {
        return repository.findBySecurityGroup(securityGroup);
    }

    public Iterable<InstanceGroup> saveAll(Iterable<InstanceGroup> instanceGroups) {
        return repository.saveAll(instanceGroups);
    }

}
