package com.sequenceiq.cloudbreak.service.hostmetadata;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.HostMetadataState;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;

@Service
public class HostMetadataService {

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    public Optional<HostMetadata> findHostInClusterByName(Long clusterId, String hostName) {
        return hostMetadataRepository.findHostInClusterByName(clusterId, hostName);
    }

    public Set<HostMetadata> findEmptyHostMetadataInHostGroup(Long hostGroupId) {
        return hostMetadataRepository.findEmptyHostsInHostGroup(hostGroupId);
    }

    public HostMetadata save(HostMetadata hostMetadata) {
        return hostMetadataRepository.save(hostMetadata);
    }

    public HostMetadata updateHostMetaDataStatus(Long id, HostMetadataState status) {
        HostMetadata hostMetadata = hostMetadataRepository.findById(id)
                .orElseThrow(notFound("HostMetadata", id));
        hostMetadata.setHostMetadataState(status);
        return hostMetadataRepository.save(hostMetadata);
    }

    public Set<HostMetadata> findHostsInCluster(Long clusterId) {
        return hostMetadataRepository.findHostsInCluster(clusterId);
    }

    public Iterable<HostMetadata> saveAll(Iterable<HostMetadata> hostMetadata) {
        return hostMetadataRepository.saveAll(hostMetadata);
    }

}
