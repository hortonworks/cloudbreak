package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.registry.Cluster;
import com.sequenceiq.periscope.rest.json.ClusterJson;

@Component
public class ClusterConverter extends AbstractConverter<ClusterJson, Cluster> {

    @Override
    public ClusterJson convert(Cluster source) {
        return new ClusterJson(source.getClusterId(), source.getHost(), source.getPort());
    }

}
