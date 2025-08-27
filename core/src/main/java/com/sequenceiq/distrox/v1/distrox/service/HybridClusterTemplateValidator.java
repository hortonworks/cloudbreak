package com.sequenceiq.distrox.v1.distrox.service;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;

@Service
public class HybridClusterTemplateValidator {

    public boolean shouldPopulate(ClusterTemplateView clusterTemplate, Boolean hybridEnvironment) {
        return clusterTemplate.getStatus().isUserManaged()
                || hybridEnvironment == null
                || hybridEnvironment == clusterTemplate.getType().isHybrid();
    }
}
