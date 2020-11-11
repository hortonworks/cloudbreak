package com.sequenceiq.mock.legacy.freeipa.response;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.DnsZone;

@Component
public class DnsZoneFindResponse extends AbstractFreeIpaResponse<Set<DnsZone>> {
    @Override
    public String method() {
        return "dnszone_find";
    }

    @Override
    protected Set<DnsZone> handleInternal(String body) {
        return Set.of();
    }
}
