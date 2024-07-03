package com.sequenceiq.cloudbreak.service.hostgroup;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.projection.HostGroupRepairView;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Service
public class HostGroupService {

    @Inject
    private HostGroupRepository hostGroupRepository;

    public Set<HostGroup> getByCluster(Long clusterId) {
        return hostGroupRepository.findHostGroupsInCluster(clusterId);
    }

    public Optional<HostGroup> findHostGroupInClusterByName(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByNameWithInstanceMetadas(clusterId, hostGroupName);
    }

    public boolean hasHostGroupInCluster(Long clusterId, String hostGroupName) {
        return hostGroupRepository.hasHostGroupInCluster(clusterId, hostGroupName);
    }

    public RecoveryMode getRecoveryMode(ClusterView cluster, String hostGroupName) {
        if (cluster != null) {
            return hostGroupRepository.getRecoveryMode(cluster.getId(), hostGroupName);
        }
        return null;
    }

    public HostGroup save(HostGroup hostGroup) {
        return hostGroupRepository.save(hostGroup);
    }

    public Optional<HostGroup> getByClusterIdAndName(Long clusterId, String instanceGroupName) {
        return hostGroupRepository.findHostGroupInClusterByNameWithInstanceMetadas(clusterId, instanceGroupName);
    }

    public Optional<HostGroupRepairView> getRepairViewByClusterIdAndName(Long clusterId, String instanceGroupName) {
        return hostGroupRepository.findHostGroupRepairViewInClusterByName(clusterId, instanceGroupName);
    }

    public Set<HostGroup> findHostGroupsInCluster(Long clusterId) {
        return hostGroupRepository.findHostGroupsInCluster(clusterId);
    }

    public void deleteAll(Iterable<HostGroup> hostGroups) {
        hostGroupRepository.deleteAll(hostGroups);
    }

    public Set<Recipe> getRecipesByHostGroups(Set<HostGroup> hostGroups) {
        return hostGroups.stream().flatMap(hostGroup -> hostGroup.getRecipes().stream()).collect(Collectors.toSet());
    }

    public Set<HostGroup> getByClusterWithRecipes(Long clusterId) {
        return hostGroupRepository.findHostGroupsInClusterWithRecipes(clusterId);
    }

    public HostGroup getByClusterIdAndNameWithRecipes(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByNameWithRecipes(clusterId, hostGroupName);
    }

    public List<Recipe> getRecipesForHostGroup(Long clusterId, String groupName) {
        return hostGroupRepository.findRecipesForHostGroup(clusterId, groupName);
    }

    public Optional<RecoveryMode> getRecoveryModeForHostGroup(Long clusterId, String groupName) {
        return hostGroupRepository.findRecoveryModeForHostGroup(clusterId, groupName);
    }

    public Set<Long> findAllHostGroupIdsByRecipeId(Long recipeId) {
        return hostGroupRepository.findAllHostGroupIdsByRecipeId(recipeId);
    }
}
