package com.sequenceiq.cloudbreak.san;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_4_3;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class LoadBalancerSANProvider {

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    public Optional<String> getLoadBalancerSAN(Stack stack) {
        checkNotNull(stack);
        checkNotNull(stack.getCluster());
        ClouderaManagerRepo clouderaManagerRepo = clusterComponentConfigProvider.getClouderaManagerRepoDetails(stack.getCluster().getId());
        if (isVersionNewerOrEqualThanLimited(clouderaManagerRepo.getVersion(), CLOUDERAMANAGER_VERSION_7_4_3)) {
            Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(stack.getId());
            if (!loadBalancers.isEmpty()) {
                Optional<LoadBalancer> loadBalancer = loadBalancerConfigService.selectLoadBalancer(loadBalancers, LoadBalancerType.PUBLIC);
                return loadBalancer.flatMap(this::getBestSANForLB);
            }
        }
        return Optional.empty();
    }

    private Optional<String> getBestSANForLB(LoadBalancer lb) {
        if (isNotBlank(lb.getFqdn())) {
            return Optional.of("DNS:" + lb.getFqdn());
        }
        if (isNotBlank(lb.getDns())) {
            return Optional.of("DNS:" + lb.getDns());
        }
        return Optional.of("IP:" + lb.getIp());
    }
}
