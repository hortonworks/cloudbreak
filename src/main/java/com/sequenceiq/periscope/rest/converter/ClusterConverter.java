package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.registry.ClusterRegistration;
import com.sequenceiq.periscope.rest.json.ClusterJson;

@Component
public class ClusterConverter extends AbstractConverter<ClusterJson, ClusterRegistration> {

    @Override
    public ClusterRegistration convert(ClusterJson source) {
        throw new UnsupportedOperationException("Cannot create cluster registration with cluster json");
    }

    @Override
    public ClusterJson convert(ClusterRegistration source) {
        return new ClusterJson(source.getClusterId(), source.getHost(), source.getPort());
    }

}
