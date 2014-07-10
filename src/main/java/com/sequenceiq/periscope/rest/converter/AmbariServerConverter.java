package com.sequenceiq.periscope.rest.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.AmbariServer;
import com.sequenceiq.periscope.rest.json.AmbariServerJson;

@Component
public class AmbariServerConverter implements Converter<AmbariServerJson, AmbariServer> {

    @Override
    public AmbariServer convert(AmbariServerJson source) {
        return new AmbariServer(source.getHost(), source.getPort(), source.getUser(), source.getPass());
    }
}
