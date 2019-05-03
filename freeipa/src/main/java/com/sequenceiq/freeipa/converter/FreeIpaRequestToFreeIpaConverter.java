package com.sequenceiq.freeipa.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.model.freeipa.FreeIpaRequest;
import com.sequenceiq.freeipa.entity.FreeIpa;

@Component
public class FreeIpaRequestToFreeIpaConverter implements Converter<FreeIpaRequest, FreeIpa> {
    @Override
    public FreeIpa convert(FreeIpaRequest source) {
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setAdminPassword(source.getAdminPassword());
        freeIpa.setDomain(source.getDomain());
        freeIpa.setHostname(source.getHostname());
        return freeIpa;
    }
}
