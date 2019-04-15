package com.sequenceiq.it.cloudbreak.newway;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;

public class ClusterTemplateUtil {

    private ClusterTemplateUtil() {

    }

    public static Set<ClusterTemplateV4Response> getResponseFromViews(Collection<ClusterTemplateViewV4Response> views) {
        return views.stream()
                .map(view -> {
                    ClusterTemplateV4Response response = new ClusterTemplateV4Response();
                    response.setStatus(view.getStatus());
                    response.setDatalakeRequired(view.getDatalakeRequired());
                    response.setId(view.getId());
                    response.setCloudPlatform(view.getCloudPlatform());
                    response.setDescription(view.getDescription());
                    response.setName(view.getName());
                    response.setType(view.getType());
                    return response;
                })
                .collect(Collectors.toSet());
    }
}
