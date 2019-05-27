package com.sequenceiq.freeipa.converter.freeipa;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerResponse;
import com.sequenceiq.freeipa.entity.FreeIpa;

@Component
public class FreeIpaToFreeIpaServerResponseConverter implements Converter<FreeIpa, FreeIpaServerResponse> {

    @Override
    public FreeIpaServerResponse convert(FreeIpa source) {
        FreeIpaServerResponse response = new FreeIpaServerResponse();
        response.setDomain(source.getDomain());
        response.setHostname(source.getHostname());
        return response;
    }
}
