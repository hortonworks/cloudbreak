package com.sequenceiq.it.cloudbreak.newway;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateResponse;
import com.sequenceiq.cloudbreak.api.model.template.ClusterTemplateViewResponse;

public class ClusterTemplateUtil {

    private ClusterTemplateUtil() {

    }

    public static Set<ClusterTemplateResponse> getResponseFromViews(Collection<ClusterTemplateViewResponse> views) {
        return views.stream()
                .map(view -> {
                    ClusterTemplateResponse response = new ClusterTemplateResponse();
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
