package com.sequenceiq.cloudbreak.service.hostmetadata;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;

@Service
public class HostMetadataService {

    @Inject
    private HostMetadataRepository repository;

    public Set<HostMetadata> findHostsInCluster(Long clusterId) {
        return repository.findHostsInCluster(clusterId);
    }

    public Set<HostMetadata> findEmptyHostsInHostGroup(Long hostGroupId) {
        return repository.findEmptyHostsInHostGroup(hostGroupId);
    }

    public Optional<HostMetadata> findHostInClusterByName(Long clusterId, String hostName) {
        return repository.findHostInClusterByName(clusterId, hostName);
    }

    public Set<HostMetadata> findHostsInClusterByName(Long clusterId, String hostName) {
        return repository.findHostsInClusterByName(clusterId, hostName);
    }

    public Iterable<HostMetadata> saveAll(Iterable<HostMetadata> hostMetadata) {
        return repository.saveAll(hostMetadata);
    }

    public void deleteAll(Iterable<HostMetadata> hostMetadata) {
        repository.deleteAll(hostMetadata);
    }

    public HostMetadata save(HostMetadata hostMetadata) {
        return repository.save(hostMetadata);
    }

    public Optional<HostMetadata> findById(Long id) {
        return repository.findById(id);
    }

    public void delete(HostMetadata hostMetadata) {
        repository.delete(hostMetadata);
    }

    public Optional<HostMetadata> getHostMetadataByClusterAndHostName(Cluster cluster, String hostName) {
        return repository.findHostInClusterByName(cluster.getId(), hostName);
    }

    public void updateHostMetaDataStatus(Cluster cluster, String hostName, HostMetadataState status) {
        Optional<HostMetadata> hostMetadata = getHostMetadataByClusterAndHostName(cluster, hostName);
        hostMetadata.ifPresent(hmd -> {
            hmd.setHostMetadataState(status);
            repository.save(hmd);
        });
    }
}
