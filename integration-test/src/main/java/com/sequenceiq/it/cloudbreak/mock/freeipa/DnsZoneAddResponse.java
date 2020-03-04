package com.sequenceiq.it.cloudbreak.mock.freeipa;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.DnsZone;

import spark.Request;
import spark.Response;

@Component
public class DnsZoneAddResponse extends AbstractFreeIpaResponse<DnsZone> {
    @Override
    public String method() {
        return "dnszone_add";
    }

    @Override
    protected DnsZone handleInternal(Request request, Response response) {
        return new DnsZone();
    }
}
