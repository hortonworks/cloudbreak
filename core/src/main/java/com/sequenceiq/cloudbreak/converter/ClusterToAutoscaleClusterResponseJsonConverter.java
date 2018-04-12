package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.AutoscaleClusterResponse;
import com.sequenceiq.cloudbreak.domain.Cluster;
import org.springframework.stereotype.Component;

@Component
public class ClusterToAutoscaleClusterResponseJsonConverter extends AbstractConversionServiceAwareConverter<Cluster, AutoscaleClusterResponse> {

    @Override
    public AutoscaleClusterResponse convert(Cluster source) {
        AutoscaleClusterResponse response = getConversionService().convert(source, AutoscaleClusterResponse.class);
        response.setPassword(source.getPassword());
        return response;
    }
}
