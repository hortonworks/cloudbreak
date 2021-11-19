package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupViewRepository;
import com.sequenceiq.cloudbreak.service.network.instancegroup.InstanceGroupNetworkService;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.CloudIdentityType;

@Service
public class InstanceGroupService {

    @Inject
    private InstanceGroupRepository repository;

    @Inject
    private InstanceGroupViewRepository viewRepository;

    @Inject
    private SecurityGroupService securityGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TemplateService templateService;

    @Inject
    private InstanceGroupNetworkService instanceGroupNetworkService;

    @Inject
    private TransactionService transactionService;

    public Set<InstanceGroup> findByStackId(Long stackId) {
        return repository.findByStackId(stackId);
    }

    public Set<InstanceGroupView> findViewByStackId(Long stackId) {
        return viewRepository.findInstanceGroupsInStack(stackId);
    }

    public Set<InstanceGroup> findNotTerminatedByStackId(Long stackId) {
        try {
            return transactionService.required(() -> {
                Set<InstanceGroup> instanceGroups = repository.findByStackId(stackId);
                instanceGroups.forEach(
                        ig -> ig.replaceInstanceMetadata(ig.getNotTerminatedInstanceMetaDataSet())
                );
                return instanceGroups;
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw new CloudbreakServiceException("Can't load instance groups for stack ID.", e);
        }
    }

    public Set<InstanceGroup> saveAll(Set<InstanceGroup> instanceGroups, Workspace workspace) {
        return instanceGroups.stream()
                .filter(ig -> ig.getSecurityGroup() != null)
                .map(ig -> {
                    ig.getSecurityGroup().getSecurityRules().forEach(sr -> sr.setSecurityGroup(ig.getSecurityGroup()));
                    securityGroupService.pureSave(ig.getSecurityGroup());
                    ig.getTemplate().setWorkspace(workspace);
                    templateService.savePure(ig.getTemplate());
                    instanceGroupNetworkService.savePure(ig.getInstanceGroupNetwork());
                    InstanceGroup instanceGroup = repository.save(ig);
                    ig.getInstanceMetaDataSet().forEach(instanceMetaDataService::save);
                    return instanceGroup;
                }).collect(Collectors.toSet());
    }

    public Optional<InstanceGroup> findOneWithInstanceMetadataByGroupNameInStack(Long stackId, String groupName) {
        return repository.findOneWithInstanceMetadataByGroupNameInStack(stackId, groupName);
    }

    public Optional<InstanceGroup> findInstanceGroupInStackByHostName(Long stackId, String hostName) {
        return repository.findInstanceGroupInStackByHostName(stackId, hostName);
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

    public void setCloudIdentityType(InstanceGroup instanceGroup, CloudIdentityType cloudIdentityType) {
        if (instanceGroup.getCloudIdentityType().isEmpty()) {
            instanceGroup.setCloudIdentityType(cloudIdentityType);
            save(instanceGroup);
        }
    }

    public Set<InstanceGroup> findByTargetGroupId(Long targetGroupId) {
        return repository.findByTargetGroupId(targetGroupId);
    }

    public InstanceGroup getPrimaryGatewayInstanceGroupByStackId(Long stackId) {
        return repository.getPrimaryGatewayInstanceGroupByStackId(stackId).orElseThrow(notFound("Gateway Instance Group for Stack", stackId));
    }

    public Set<InstanceGroup> getByStackAndFetchTemplates(Long stackId) {
        return repository.getByStackAndFetchTemplates(stackId);
    }
}
