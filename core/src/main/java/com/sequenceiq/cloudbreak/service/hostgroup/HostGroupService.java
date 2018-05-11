package com.sequenceiq.cloudbreak.service.hostgroup;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.repository.ConstraintRepository;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;

@Service
public class HostGroupService {

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    private ConstraintRepository constraintRepository;

    @Inject
    private TransactionService transactionService;

    public Set<HostGroup> getByCluster(Long clusterId) {
        return hostGroupRepository.findHostGroupsInCluster(clusterId);
    }

    public HostGroup getByClusterIdAndName(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByName(clusterId, hostGroupName);
    }

    public HostMetadata getHostMetadataByClusterAndHostName(Cluster cluster, String hostName) {
        return hostMetadataRepository.findHostInClusterByName(cluster.getId(), hostName);
    }

    public HostGroup getByClusterAndHostName(Cluster cluster, String hostName) {
        HostMetadata hostMetadata = getHostMetadataByClusterAndHostName(cluster, hostName);
        if (hostMetadata != null) {
            String hostGroupName = hostMetadata.getHostGroup().getName();
            return getByClusterIdAndName(cluster.getId(), hostGroupName);
        } else {
            return null;
        }
    }

    public HostGroup save(HostGroup hostGroup) {
        return hostGroupRepository.save(hostGroup);
    }

    public Set<HostMetadata> findEmptyHostMetadataInHostGroup(Long hostGroupId) {
        return hostMetadataRepository.findEmptyHostsInHostGroup(hostGroupId);
    }

    public HostGroup getByClusterIdAndInstanceGroupName(Long clusterId, String instanceGroupName) {
        return hostGroupRepository.findHostGroupsByInstanceGroupName(clusterId, instanceGroupName);
    }

    public HostMetadata updateHostMetaDataStatus(Long id, HostMetadataState status) {
        HostMetadata metaData = hostMetadataRepository.findOne(id);
        metaData.setHostMetadataState(status);
        return hostMetadataRepository.save(metaData);
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
