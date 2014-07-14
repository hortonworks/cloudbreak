package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.registry.ClusterRegistration;
import com.sequenceiq.periscope.rest.json.ClusterJson;

@Component
public class ClusterConverter extends AbstractConverter<ClusterJson, ClusterRegistration> {

    @Override
    public ClusterJson convert(ClusterRegistration source) {
        return new ClusterJson(source.getClusterId(), source.getHost(), source.getPort());
    }

}
