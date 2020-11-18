package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.repository.LoadBalancerRepository;

@Service
public class LoadBalancerPersistenceService {

    @Inject
    private LoadBalancerRepository repository;

    public LoadBalancer save(LoadBalancer loadBalancer) {
        return repository.save(loadBalancer);
    }

    public Iterable<LoadBalancer> saveAll(Iterable<LoadBalancer> loadBalancers) {
        return repository.saveAll(loadBalancers);
    }

    public Set<LoadBalancer> findByStackId(Long stackId) {
        return repository.findByStackId(stackId);
    }
}
