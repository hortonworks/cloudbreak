package com.sequenceiq.cloudbreak.service.cluster;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.HostService;
import com.sequenceiq.cloudbreak.repository.HostServiceRepository;

@Service
@Transactional
public class HostServiceService {

    @Inject
    private HostServiceRepository hostServiceRepository;

    @Transactional(Transactional.TxType.NEVER)
    public Iterable<HostService> save(List<HostService> hostServices) {
        return hostServiceRepository.save(hostServices);
    }

    @Transactional(Transactional.TxType.NEVER)
    public Set<HostService> findContainersInCluster(Long clusterId) {
        return hostServiceRepository.findServicesInCluster(clusterId);
    }
}
