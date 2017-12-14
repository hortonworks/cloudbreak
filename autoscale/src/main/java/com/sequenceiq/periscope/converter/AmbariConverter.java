package com.sequenceiq.periscope.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.api.model.AutoscaleClusterRequest;
import com.sequenceiq.periscope.domain.Ambari;

@Component
public class AmbariConverter extends AbstractConverter<AutoscaleClusterRequest, Ambari> {

    @Override
    public Ambari convert(AutoscaleClusterRequest source) {
        return new Ambari(source.getHost(), source.getPort(), source.getUser(), source.getPass());
    }

}
