package com.sequenceiq.cloudbreak.service.idbroker;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.repository.IdBrokerRepository;

@Service
public class IdBrokerService {

    @Inject
    private IdBrokerRepository repository;

    public IdBroker save(IdBroker idBroker) {
        return repository.save(idBroker);
    }

    public IdBroker getByCluster(Cluster cluster) {
        return repository.findByClusterId(cluster.getId());
    }

}
