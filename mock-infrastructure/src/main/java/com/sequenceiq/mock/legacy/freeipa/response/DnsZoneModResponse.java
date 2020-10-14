package com.sequenceiq.mock.legacy.freeipa.response;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.DnsZone;

@Component
public class DnsZoneModResponse extends AbstractFreeIpaResponse<DnsZone> {
    @Override
    public String method() {
        return "dnszone_mod";
    }

    @Override
    protected DnsZone handleInternal(String body) {
        DnsZone dnsZone = new DnsZone();
        dnsZone.setIdnsname("");
        return dnsZone;
    }
}
