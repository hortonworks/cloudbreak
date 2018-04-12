package com.sequenceiq.cloudbreak.service.cluster;

import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.repository.ContainerRepository;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import java.util.Set;

@Service
@Transactional
public class ContainerService {

    @Inject
    private ContainerRepository containerRepository;

    @Transactional(TxType.NEVER)
    public Iterable<Container> save(Iterable<Container> containers) {
        return containerRepository.save(containers);
    }

    @Transactional(TxType.NEVER)
    public Set<Container> findContainersInCluster(Long clusterId) {
        return containerRepository.findContainersInCluster(clusterId);
    }
}
