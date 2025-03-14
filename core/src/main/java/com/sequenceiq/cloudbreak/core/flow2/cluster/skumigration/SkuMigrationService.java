package com.sequenceiq.cloudbreak.core.flow2.cluster.skumigration;

import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.common.api.type.LoadBalancerSku;

@Service
public class SkuMigrationService {

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    public void updateSkuToStandard(Set<LoadBalancer> loadBalancers) {
        loadBalancers.forEach(loadBalancer -> {
            loadBalancer.setSku(LoadBalancerSku.STANDARD);
        });
        loadBalancerPersistenceService.saveAll(loadBalancers);
    }

}
