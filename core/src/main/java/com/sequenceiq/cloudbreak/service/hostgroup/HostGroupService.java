package com.sequenceiq.cloudbreak.service.hostgroup;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;

@Service
public class HostGroupService {

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private TransactionService transactionService;

    public Set<HostGroup> getByCluster(Long clusterId) {
        return hostGroupRepository.findHostGroupsInCluster(clusterId);
    }

    public Optional<HostGroup> findHostGroupInClusterByName(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByNameWithInstanceMetadas(clusterId, hostGroupName);
    }

    public HostGroup save(HostGroup hostGroup) {
        return hostGroupRepository.save(hostGroup);
    }

    public Optional<HostGroup> getByClusterIdAndName(Long clusterId, String instanceGroupName) {
        return hostGroupRepository.findHostGroupInClusterByNameWithInstanceMetadas(clusterId, instanceGroupName);
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

    public Set<Recipe> getRecipesByCluster(Long clusterId) {
        return getByClusterWithRecipes(clusterId).stream().flatMap(hostGroup -> hostGroup.getRecipes().stream()).collect(Collectors.toSet());
    }

    public Set<HostGroup> getByClusterWithRecipes(Long clusterId) {
        return hostGroupRepository.findHostGroupsInClusterWithRecipes(clusterId);
    }

    public HostGroup getByClusterIdAndNameWithRecipes(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByNameWithRecipes(clusterId, hostGroupName);
    }

}
