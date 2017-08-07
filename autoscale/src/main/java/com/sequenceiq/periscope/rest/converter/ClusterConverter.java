package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ClusterJson;
import com.sequenceiq.periscope.domain.Cluster;

@Component
public class ClusterConverter extends AbstractConverter<ClusterJson, Cluster> {

    @Override
    public ClusterJson convert(Cluster source) {
        ClusterJson json = new ClusterJson();
        json.setId(source.getId());
        json.setStackId(source.getStackId());
        json.setHost(source.getHost());
        json.setPort(source.getPort());
        json.setState(source.getState().name());
        json.setAutoscalingEnabled(source.isAutoscalingEnabled());
        return json;
    }

}
