package com.sequenceiq.cloudbreak.service.stack;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Service
public class InstanceGroupService {

    @Inject
    private InstanceGroupRepository repository;

    @Inject
    private SecurityGroupService securityGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TemplateService templateService;

    public Set<InstanceGroup> findByStackId(Long stackId) {
        return repository.findByStackId(stackId);
    }

    public Set<InstanceGroup> saveAll(Set<InstanceGroup> instanceGroups, Workspace workspace) {
        return instanceGroups.stream()
                .filter(ig -> ig.getSecurityGroup() != null)
                .map(ig -> {
                    ig.getSecurityGroup().getSecurityRules().forEach(sr -> sr.setSecurityGroup(ig.getSecurityGroup()));
                    securityGroupService.pureSave(ig.getSecurityGroup());
                    ig.getTemplate().setWorkspace(workspace);
                    templateService.savePure(ig.getTemplate());
                    InstanceGroup instanceGroup = repository.save(ig);
                    ig.getInstanceMetaDataSet().forEach(instanceMetaDataService::save);
                    return instanceGroup;
                }).collect(Collectors.toSet());
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
