package com.sequenceiq.cloudbreak.service.hostgroup;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.service.hostmetadata.HostMetadataService;

@Service
public class HostGroupService {

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private HostMetadataService hostMetadataService;

    public Set<HostGroup> getByCluster(Long clusterId) {
        return hostGroupRepository.findHostGroupsInCluster(clusterId);
    }

    public Optional<HostGroup> findHostGroupInClusterByName(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByName(clusterId, hostGroupName);
    }

    public HostGroup save(HostGroup hostGroup) {
        return hostGroupRepository.save(hostGroup);
    }

    public Set<HostMetadata> findEmptyHostMetadataInHostGroup(Long hostGroupId) {
        return hostMetadataService.findEmptyHostsInHostGroup(hostGroupId);
    }

    public Optional<HostGroup> getByClusterIdAndInstanceGroupName(Long clusterId, String instanceGroupName) {
        return hostGroupRepository.findHostGroupsByInstanceGroupName(clusterId, instanceGroupName);
    }

    public void updateHostMetaDataStatus(Long id, HostMetadataState status) {
        HostMetadata hostMetadata = hostMetadataService.findById(id)
                .orElseThrow(notFound("HostMetadata", id));
        hostMetadata.setHostMetadataState(status);
        hostMetadataService.save(hostMetadata);
    }

    public Set<HostGroup> findHostGroupsInCluster(Long clusterId) {
        return hostGroupRepository.findHostGroupsInCluster(clusterId);
    }

    public void deleteAll(Iterable<HostGroup> hostGroups) {
        hostGroupRepository.deleteAll(hostGroups);
    }

    public Set<HostGroup> findAllHostGroupsByRecipe(Long recipeId) {
        return hostGroupRepository.findAllHostGroupsByRecipe(recipeId);
    }

}
