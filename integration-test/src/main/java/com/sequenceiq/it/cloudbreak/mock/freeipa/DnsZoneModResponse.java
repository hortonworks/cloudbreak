package com.sequenceiq.it.cloudbreak.mock.freeipa;

import com.sequenceiq.freeipa.client.model.DnsZone;
import org.springframework.stereotype.Component;
import spark.Request;
import spark.Response;

@Component
public class DnsZoneModResponse extends AbstractFreeIpaResponse<DnsZone> {
    @Override
    public String method() {
        return "dnszone_mod";
    }

    @Override
    protected DnsZone handleInternal(Request request, Response response) {
        DnsZone dnsZone = new DnsZone();
        dnsZone.setIdnsname("");
        return dnsZone;
    }
}
