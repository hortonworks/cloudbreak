package com.sequenceiq.cloudbreak.converter.stack.cluster;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.AutoscaleClusterResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@Component
public class ClusterToAutoscaleClusterResponseJsonConverter extends AbstractConversionServiceAwareConverter<Cluster, AutoscaleClusterResponse> {

    @Inject
    private SecretService secretService;

    @Override
    public AutoscaleClusterResponse convert(Cluster source) {
        AutoscaleClusterResponse response = getConversionService().convert(source, AutoscaleClusterResponse.class);
        response.setPassword(secretService.get(source.getCloudbreakAmbariPassword()));
        response.setUserName(secretService.get(source.getCloudbreakAmbariUser()));
        return response;
    }
}
