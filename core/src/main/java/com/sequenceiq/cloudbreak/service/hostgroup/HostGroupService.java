package com.sequenceiq.cloudbreak.service.hostgroup;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.repository.ConstraintRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;

@Service
public class HostGroupService {

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private ConstraintRepository constraintRepository;

    @Inject
    private TransactionService transactionService;

    public Set<HostGroup> findHostGroupsInCluster(Long clusterId) {
        return hostGroupRepository.findHostGroupsInCluster(clusterId);
    }

    public void deleteAll(Iterable<HostGroup> hostGroups) {
        hostGroupRepository.deleteAll(hostGroups);
    }

    public Optional<HostGroup> findHostGroupInClusterByName(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByName(clusterId, hostGroupName);
    }

    public HostGroup save(HostGroup hostGroup) {
        return hostGroupRepository.save(hostGroup);
    }

    public Set<HostGroup> findAllHostGroupsByRecipe(Long id) {
        return hostGroupRepository.findAllHostGroupsByRecipe(id);
    }

    public Optional<HostGroup> getByClusterIdAndInstanceGroupName(Long clusterId, String instanceGroupName) {
        return hostGroupRepository.findHostGroupsByInstanceGroupName(clusterId, instanceGroupName);
    }

    public Set<HostGroup> saveOrUpdateWithMetadata(Collection<HostGroup> hostGroups, Cluster cluster) throws TransactionExecutionException {
        Set<HostGroup> result = new HashSet<>(hostGroups.size());
        return transactionService.required(() -> {
            for (HostGroup hg : hostGroups) {
                hg.setCluster(cluster);
                hg.setConstraint(constraintRepository.save(hg.getConstraint()));
                result.add(hostGroupRepository.save(hg));
            }
            return result;
        });
    }

}
