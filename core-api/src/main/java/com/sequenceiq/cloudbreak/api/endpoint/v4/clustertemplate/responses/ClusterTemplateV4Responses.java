package com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses;

import java.util.HashSet;
import java.util.Set;

import io.swagger.annotations.ApiModel;

@ApiModel
public class ClusterTemplateV4Responses {

    private Set<ClusterTemplateV4Response> clusterTemplates = new HashSet<>();

    public Set<ClusterTemplateV4Response> getClusterTemplates() {
        return clusterTemplates;
    }

    public void setClusterTemplates(Set<ClusterTemplateV4Response> clusterTemplates) {
        this.clusterTemplates = clusterTemplates;
    }

    public static final ClusterTemplateV4Responses clusterTemplateV4Responses(Set<ClusterTemplateV4Response> clusterTemplates) {
        ClusterTemplateV4Responses clusterTemplateV4Responses = new ClusterTemplateV4Responses();
        clusterTemplateV4Responses.setClusterTemplates(clusterTemplates);
        return clusterTemplateV4Responses;
    }
}
