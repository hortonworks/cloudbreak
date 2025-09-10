package com.sequenceiq.cloudbreak.service.stack;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.repository.LoadBalancerRepository;

@Service
public class LoadBalancerPersistenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalancerPersistenceService.class);

    @Inject
    private LoadBalancerRepository repository;

    @Inject
    private TransactionService transactionService;

    @Inject
    private TargetGroupPersistenceService targetGroupPersistenceService;

    public LoadBalancer save(LoadBalancer loadBalancer) {
        return repository.save(loadBalancer);
    }

    public void deleteByStackId(Long stackId) throws TransactionService.TransactionExecutionException {
        transactionService.required(() -> {
            Set<LoadBalancer> loadBalancers = repository.findByStackId(stackId);
            for (LoadBalancer loadBalancer : loadBalancers) {
                LOGGER.debug("Cleanup targetgroups for loadBalancer {}", loadBalancer.getId());
                for (TargetGroup targetGroup : loadBalancer.getTargetGroupSet()) {
                    targetGroupPersistenceService.delete(targetGroup.getId());
                }
            }
            repository.deleteByStackId(stackId);
        });
    }

    public Iterable<LoadBalancer> saveAll(Iterable<LoadBalancer> loadBalancers) {
        return repository.saveAll(loadBalancers);
    }

    public Set<LoadBalancer> findByStackId(Long stackId) {
        return repository.findByStackId(stackId);
    }
}
