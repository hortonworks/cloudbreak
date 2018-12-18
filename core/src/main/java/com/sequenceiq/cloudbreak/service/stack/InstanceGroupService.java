package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Service
public class InstanceGroupService {

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private SecurityGroupService securityGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TemplateService templateService;

    public Set<InstanceGroup> findByStackId(Long stackId) {
        return instanceGroupRepository.findByStackId(stackId);
    }

    public Set<InstanceGroup> saveAll(Set<InstanceGroup> instanceGroups, Workspace workspace) {
        return instanceGroups.stream()
                .filter(ig -> ig.getSecurityGroup() != null)
                .map(ig -> {
                    ig.getSecurityGroup().getSecurityRules().forEach(sr -> sr.setSecurityGroup(ig.getSecurityGroup()));
                    securityGroupService.pureSave(ig.getSecurityGroup());
                    ig.getTemplate().setWorkspace(workspace);
                    templateService.savePure(ig.getTemplate());
                    InstanceGroup instanceGroup = instanceGroupRepository.save(ig);
                    ig.getInstanceMetaDataSet().forEach(instanceMetaDataService::pureSave);
                    return instanceGroup;
                }).collect(Collectors.toSet());
    }
}
