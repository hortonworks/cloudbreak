package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.Ambari;
import com.sequenceiq.periscope.rest.json.AmbariJson;

@Component
public class AmbariConverter extends AbstractConverter<AmbariJson, Ambari> {

    @Override
    public Ambari convert(AmbariJson source) {
        return new Ambari(source.getHost(), source.getPort(), source.getUser(), source.getPass());
    }

    @Override
    public AmbariJson convert(Ambari source) {
        throw new UnsupportedOperationException("Cannot create ambari json from ambari entity.");
    }
}
