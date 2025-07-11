package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.AvailabilityZoneConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.AvailabilityZone;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupViewRepository;
import com.sequenceiq.cloudbreak.service.network.instancegroup.InstanceGroupNetworkService;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.view.AvailabilityZoneView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.model.CloudIdentityType;

@Service
public class InstanceGroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceGroupService.class);

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

    @Inject
    private AvailabilityZoneConverter availabilityZoneConverter;

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
                    if (ig.getInstanceGroupNetwork() != null) {
                        instanceGroupNetworkService.savePure(ig.getInstanceGroupNetwork());
                    }
                    InstanceGroup instanceGroup = repository.save(ig);
                    ig.getInstanceMetaData().forEach(instanceMetaDataService::save);
                    return instanceGroup;
                }).collect(Collectors.toSet());
    }

    public Optional<InstanceGroup> findOneWithInstanceMetadataByGroupNameInStack(Long stackId, String groupName) {
        return repository.findOneWithInstanceMetadataByGroupNameInStack(stackId, groupName);
    }

    public Optional<InstanceGroup> getInstanceGroupWithTemplateAndInstancesByGroupNameInStack(Long stackId, String groupName) {
        return repository.getInstanceGroupWithTemplateAndInstancesByGroupNameInStack(stackId, groupName);
    }

    public Optional<InstanceGroup> findOneByStackIdAndGroupName(Long stackId, String groupName) {
        return repository.findOneByStackIdAndGroupName(stackId, groupName);
    }

    public Set<String> findAvailabilityZonesByStackIdAndGroupId(Long groupId) {
        return repository.findAvailabilityZonesByStackIdAndGroupId(groupId)
                .stream()
                .collect(Collectors.toSet());
    }

    public Optional<com.sequenceiq.cloudbreak.view.InstanceGroupView> findInstanceGroupViewByStackIdAndGroupName(Long stackId, String groupName) {
        return Optional.ofNullable(repository.findInstanceGroupViewByStackIdAndGroupName(stackId, groupName).orElse(null));
    }

    public List<com.sequenceiq.cloudbreak.view.InstanceGroupView> findAllInstanceGroupViewByStackIdAndGroupName(Long stackId, Collection<String> groupNames) {
        return new ArrayList<>(repository.findAllInstanceGroupViewByStackIdAndGroupNames(stackId, groupNames));
    }

    public InstanceGroup save(InstanceGroup instanceGroup) {
        return repository.save(instanceGroup);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Set<InstanceGroup> findBySecurityGroup(SecurityGroup securityGroup) {
        return repository.findBySecurityGroup(securityGroup);
    }

    public Iterable<InstanceGroup> saveAll(Iterable<InstanceGroup> instanceGroups) {
        return repository.saveAll(instanceGroups);
    }

    public void setCloudIdentityType(com.sequenceiq.cloudbreak.view.InstanceGroupView instanceGroup, CloudIdentityType cloudIdentityType) {
        if (instanceGroup.getCloudIdentityType().isEmpty()) {
            Map<String, Object> attributeMap = instanceGroup.getAttributes().getMap();
            attributeMap.put(InstanceGroup.IDENTITY_TYPE_ATTRIBUTE, cloudIdentityType);
            Json attributes = new Json(attributeMap);
            repository.updateAttributes(instanceGroup.getId(), attributes);
        }
    }

    public List<com.sequenceiq.cloudbreak.view.InstanceGroupView> findByTargetGroupId(Long targetGroupId) {
        return new ArrayList<>(repository.findByTargetGroupId(targetGroupId));
    }

    public InstanceGroup getPrimaryGatewayInstanceGroupByStackId(Long stackId) {
        return repository.getPrimaryGatewayInstanceGroupByStackId(stackId).orElseThrow(notFound("Gateway Instance Group for Stack", stackId));
    }

    public InstanceGroup getPrimaryGatewayInstanceGroupWithTemplateByStackId(Long stackId) {
        return repository.getPrimaryGatewayInstanceGroupWithTemplateByStackId(stackId).orElseThrow(notFound("Gateway Instance Group for Stack", stackId));
    }

    public Set<InstanceGroup> getByStackAndFetchTemplates(Long stackId) {
        return repository.getByStackAndFetchTemplates(stackId);
    }

    public List<com.sequenceiq.cloudbreak.view.InstanceGroupView> getInstanceGroupViewByStackId(Long stackId) {
        return new ArrayList<>(repository.findInstanceGroupViewByStackId(stackId));
    }

    public Map<Long, List<AvailabilityZoneView>> getAvailabilityZonesByStackId(Long stackId) {
        List<AvailabilityZoneView> availabilityZones = repository.findAvailabilityZonesByStackId(stackId);
        return availabilityZones.stream().collect(Collectors.groupingBy(AvailabilityZoneView::getInstanceGroupId));
    }

    public InstanceGroup saveEnvironmentAvailabilityZones(InstanceGroup instanceGroup, Set<String> environmentZones) {
        LOGGER.info("Saving availability zones '{}' on group('{}') and instance group network level.", environmentZones, instanceGroup.getGroupName());
        InstanceGroupNetwork instanceGroupNetwork = instanceGroup.getInstanceGroupNetwork();
        if (instanceGroupNetwork == null) {
            instanceGroupNetwork = new InstanceGroupNetwork();
            instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        }
        Json extendedGroupNetworkAttributes = availabilityZoneConverter.getJsonAttributesWithAvailabilityZones(environmentZones,
                instanceGroupNetwork.getAttributes());
        instanceGroupNetwork.setAttributes(extendedGroupNetworkAttributes);
        instanceGroupNetworkService.savePure(instanceGroupNetwork);

        Set<AvailabilityZone> availabilityZones = environmentZones.stream().map(zone -> {
            AvailabilityZone availabilityZone = new AvailabilityZone();
            availabilityZone.setAvailabilityZone(zone);
            availabilityZone.setInstanceGroup(instanceGroup);
            return availabilityZone;
        }).collect(Collectors.toSet());
        instanceGroup.setAvailabilityZones(availabilityZones);
        return repository.save(instanceGroup);
    }

    public Set<InstanceGroupView> getInstanceGroupViewByStackIds(Set<Long> stackIds) {
        return viewRepository.findInstanceGroupsInStacks(stackIds);
    }
}
