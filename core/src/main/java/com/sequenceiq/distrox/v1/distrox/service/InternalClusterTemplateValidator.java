package com.sequenceiq.distrox.v1.distrox.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState.INTERNAL;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.view.ClusterTemplateView;

@Service
public class InternalClusterTemplateValidator {

    public boolean shouldPopulate(ClusterTemplate clusterTemplate, boolean internalTenant) {
        if (isInternalTemplateInNotInternalTenant(internalTenant, clusterTemplate.getFeatureState())) {
            return false;
        }
        return true;
    }

    public boolean shouldPopulate(ClusterTemplateView clusterTemplateView, boolean internalTenant) {
        if (isInternalTemplateInNotInternalTenant(internalTenant, clusterTemplateView.getFeatureState())) {
            return false;
        }
        return true;
    }

    public boolean shouldPopulate(DefaultClusterTemplateV4Request defaultClusterTemplateV4Request, boolean internalTenant) {
        if (isInternalTemplateInNotInternalTenant(internalTenant, defaultClusterTemplateV4Request.getFeatureState())) {
            return false;
        }
        return true;
    }

    public boolean isInternalTemplateInNotInternalTenant(boolean internalTenant, FeatureState featureState) {
        return !internalTenant && INTERNAL.equals(featureState);
    }
}
