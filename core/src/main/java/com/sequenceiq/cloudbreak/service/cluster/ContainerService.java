package com.sequenceiq.cloudbreak.service.cluster;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.repository.ContainerRepository;

@Service
@Transactional
public class ContainerService {

    @Inject
    private ContainerRepository containerRepository;

    @Transactional(javax.transaction.Transactional.TxType.NEVER)
    public Iterable<Container> save(List<Container> containers) {
        return containerRepository.save(containers);
    }

    @Transactional(javax.transaction.Transactional.TxType.NEVER)
    public Set<Container> findContainersInCluster(Long clusterId) {
        return containerRepository.findContainersInCluster(clusterId);
    }
}
