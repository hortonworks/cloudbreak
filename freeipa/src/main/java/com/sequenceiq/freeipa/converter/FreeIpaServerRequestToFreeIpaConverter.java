package com.sequenceiq.freeipa.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.entity.FreeIpa;

@Component
public class FreeIpaServerRequestToFreeIpaConverter implements Converter<FreeIpaServerRequest, FreeIpa> {
    @Override
    public FreeIpa convert(FreeIpaServerRequest source) {
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setAdminPassword(source.getAdminPassword());
        freeIpa.setDomain(source.getDomain());
        freeIpa.setHostname(source.getHostname());
        return freeIpa;
    }
}
