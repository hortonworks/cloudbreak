package com.sequenceiq.cloudbreak.service.hostgroup;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;

@Service
public class HostGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostGroupService.class);

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private TransactionService transactionService;

    public Set<HostGroup> getByCluster(Long clusterId) {
        return hostGroupRepository.findHostGroupsInCluster(clusterId);
    }

    public HostGroup getByClusterIdAndName(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByNameWithInstanceMetadas(clusterId, hostGroupName);
    }

    public HostGroup save(HostGroup hostGroup) {
        return hostGroupRepository.save(hostGroup);
    }

    public HostGroup getByClusterIdAndInstanceGroupName(Long clusterId, String instanceGroupName) {
        return hostGroupRepository.findHostGroupsByInstanceGroupName(clusterId, instanceGroupName);
    }

    public Set<HostGroup> saveOrUpdateWithMetadata(Collection<HostGroup> hostGroups, Cluster cluster) throws TransactionExecutionException {
        Set<HostGroup> result = new HashSet<>(hostGroups.size());
        return transactionService.required(() -> {
            for (HostGroup hg : hostGroups) {
                hg.setCluster(cluster);
                result.add(hostGroupRepository.save(hg));
            }
            return result;
        });
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
