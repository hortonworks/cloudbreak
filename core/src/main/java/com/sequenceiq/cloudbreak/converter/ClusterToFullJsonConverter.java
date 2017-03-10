package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ClusterFullResponse;
import com.sequenceiq.cloudbreak.domain.Cluster;

@Component
public class ClusterToFullJsonConverter extends AbstractConversionServiceAwareConverter<Cluster, ClusterFullResponse> {

    @Inject
    private ClusterToJsonConverter clusterToJsonConverter;

    @Override
    public ClusterFullResponse convert(Cluster source) {
        try {
            ClusterFullResponse response = clusterToJsonConverter.convert(source, ClusterFullResponse.class);
            response.setPassword(source.getPassword());
            return response;
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
}
