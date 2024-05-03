package com.sequenceiq.freeipa.converter.freeipa;

import org.springframework.stereotype.Component;

import com.sequenceiq.common.model.OsType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.dto.SidGeneration;
import com.sequenceiq.freeipa.entity.FreeIpa;

@Component
public class FreeIpaServerRequestToFreeIpaConverter {

    public FreeIpa convert(FreeIpaServerRequest source, String osType) {
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setAdminPassword(source.getAdminPassword());
        freeIpa.setDomain(source.getDomain());
        freeIpa.setHostname(source.getHostname());
        freeIpa.setAdminGroupName(source.getAdminGroupName());
        freeIpa.setSidGeneration(OsType.CENTOS7.getOsType().equalsIgnoreCase(osType) ? SidGeneration.DISABLED : SidGeneration.ENABLED);
        return freeIpa;
    }
}
