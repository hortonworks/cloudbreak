package com.sequenceiq.freeipa.service.loadbalancer;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.entity.LoadBalancer;
import com.sequenceiq.freeipa.repository.LoadBalancerRepository;

@Service
public class FreeIpaLoadBalancerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaLoadBalancerService.class);

    @Inject
    private LoadBalancerRepository loadBalancerRepository;

    public Optional<LoadBalancer> findByStackId(Long stackId) {
        return loadBalancerRepository.findByStackId(stackId);
    }

    public LoadBalancer getByStackId(Long stackId) {
        return findByStackId(stackId).orElseThrow(() -> new NotFoundException("FreeIPA load balancer not found"));
    }

    public void save(LoadBalancer loadBalancer) {
        LOGGER.debug("Persisting FreeIPA load balancer {}", loadBalancer);
        loadBalancerRepository.save(loadBalancer);
    }

    public void delete(Long stackId) {
        LOGGER.debug("Deleting load balancer from database");
        loadBalancerRepository.deleteByStackId(stackId);
    }

}
