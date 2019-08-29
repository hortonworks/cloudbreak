package com.sequenceiq.cloudbreak.service.hostgroup;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.repository.HostGroupRepository;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;

@Service
public class HostGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostGroupService.class);

    @Inject
    private HostGroupRepository hostGroupRepository;

    @Inject
    private HostMetadataRepository hostMetadataRepository;

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
        HostMetadata hostMetadata = hostMetadataRepository.findById(id)
                .orElseThrow(notFound("HostMetadata", id));
        hostMetadata.setHostMetadataState(status);
        return hostMetadataRepository.save(hostMetadata);
    }

    public void updateHostMetaDataStatus(Cluster cluster, String hostName, HostMetadataState status) {
        HostMetadata hostMetadata = getHostMetadataByClusterAndHostName(cluster, hostName);
        if (hostMetadata != null) {
            hostMetadata.setHostMetadataState(status);
            hostMetadataRepository.save(hostMetadata);
        }
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

    public Set<HostGroup> getByClusterWithRecipes(Long clusterId) {
        return hostGroupRepository.findHostGroupsInClusterWithRecipes(clusterId);
    }

    public HostGroup getByClusterIdAndNameWithRecipes(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByNameWithRecipes(clusterId, hostGroupName);
    }

    public Long countByClusterIdAndName(Long id, String hostGroupName) {
        return hostMetadataRepository.countByClusterIdAndHostGroupName(id, hostGroupName);
    }

    public HostGroup getByClusterIdAndNameWithHostMetadata(Long clusterId, String hostGroupName) {
        return hostGroupRepository.findHostGroupInClusterByNameWithHostMetadata(clusterId, hostGroupName);
    }

    public Set<HostGroup> getByClusterWithRecipesAndHostmetadata(Long clusterId) {
        return hostGroupRepository.findHostGroupsInClusterWithRecipesAndHostmetadata(clusterId);
    }
}
