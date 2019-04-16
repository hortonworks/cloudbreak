package com.sequenceiq.cloudbreak.service.cluster;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Container;
import com.sequenceiq.cloudbreak.repository.ContainerRepository;

@Service
public class ContainerService {

    @Inject
    private ContainerRepository containerRepository;

    public Iterable<Container> save(Iterable<Container> containers) {
        return containerRepository.saveAll(containers);
    }

    public Set<Container> findContainersInCluster(Long clusterId) {
        return containerRepository.findContainersInCluster(clusterId);
    }

    public void deleteAll(Iterable<Container> containers) {
        containerRepository.deleteAll(containers);
    }

}
