package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AutoscaleClusterResponse;
import com.sequenceiq.cloudbreak.domain.Cluster;

@Component
public class ClusterToAutoscaleClusterResponseJsonConverter extends AbstractConversionServiceAwareConverter<Cluster, AutoscaleClusterResponse> {

    @Inject
    private ClusterToJsonConverter clusterToJsonConverter;

    @Override
    public AutoscaleClusterResponse convert(Cluster source) {
        try {
            AutoscaleClusterResponse response = clusterToJsonConverter.convert(source, AutoscaleClusterResponse.class);
            response.setPassword(source.getPassword());
            return response;
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
