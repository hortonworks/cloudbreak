package com.sequenceiq.periscope.rest.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.Ambari;
import com.sequenceiq.periscope.rest.json.AmbariJson;

@Component
public class AmbariJsonToAmbariConverter implements Converter<AmbariJson, Ambari> {

    @Override
    public Ambari convert(AmbariJson source) {
        return new Ambari(source.getHost(), source.getPort(), source.getUser(), source.getPass());
    }
}
