package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AutoscaleClusterResponse;
import com.sequenceiq.cloudbreak.domain.Cluster;

@Component
public class ClusterToAutoscaleClusterResponseJsonConverter extends AbstractConversionServiceAwareConverter<Cluster, AutoscaleClusterResponse> {

    @Override
    public AutoscaleClusterResponse convert(Cluster source) {
        AutoscaleClusterResponse response = getConversionService().convert(source, AutoscaleClusterResponse.class);
        response.setPassword(source.getPassword());
        return response;
    }
}
