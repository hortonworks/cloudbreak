package com.sequenceiq.cloudbreak.converter.stack.cluster;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AutoscaleClusterResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Component
public class ClusterToAutoscaleClusterResponseJsonConverter extends AbstractConversionServiceAwareConverter<Cluster, AutoscaleClusterResponse> {

    @Override
    public AutoscaleClusterResponse convert(Cluster source) {
        AutoscaleClusterResponse response = getConversionService().convert(source, AutoscaleClusterResponse.class);
        response.setPassword(source.getCloudbreakAmbariPassword().getRaw());
        response.setUserName(source.getCloudbreakAmbariUser().getRaw());
        return response;
    }
}
