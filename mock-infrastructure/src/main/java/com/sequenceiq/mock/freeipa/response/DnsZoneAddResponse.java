package com.sequenceiq.mock.freeipa.response;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.DnsZone;

@Component
public class DnsZoneAddResponse extends AbstractFreeIpaResponse<DnsZone> {
    @Override
    public String method() {
        return "dnszone_add";
    }

    @Override
    protected DnsZone handleInternal(String body) {
        return new DnsZone();
    }
}
