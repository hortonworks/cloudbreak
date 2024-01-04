package com.sequenceiq.cloudbreak.san;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_11;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.common.api.type.LoadBalancerType;

@Service
public class LoadBalancerSANProvider {

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    public Optional<String> getLoadBalancerSAN(Long stackId, Blueprint blueprint) {
        CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(blueprint.getBlueprintJsonText());
        String cdhVersion = cmTemplateProcessor.getStackVersion();
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_11)) {
            Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(stackId);
            if (!loadBalancers.isEmpty()) {
                Optional<LoadBalancer> loadBalancer = loadBalancerConfigService.selectLoadBalancerForFrontend(loadBalancers, LoadBalancerType.PUBLIC);
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
        if (isNotBlank(lb.getIp())) {
            return Optional.of("IP:" + lb.getIp());
        }
        return Optional.empty();
    }
}
