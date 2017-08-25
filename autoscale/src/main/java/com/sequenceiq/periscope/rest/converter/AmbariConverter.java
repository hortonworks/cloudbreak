package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.ClusterRequestJson;
import com.sequenceiq.periscope.domain.Ambari;

@Component
public class AmbariConverter extends AbstractConverter<ClusterRequestJson, Ambari> {

    @Override
    public Ambari convert(ClusterRequestJson source) {
        return new Ambari(source.getHost(), source.getPort(), source.getUser(), source.getPass());
    }

}
