package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.DnsZone;

import spark.Request;
import spark.Response;

@Component
public class DnsZoneFindResponse extends AbstractFreeIpaResponse<Set<DnsZone>> {
    @Override
    public String method() {
        return "dnszone_find";
    }

    @Override
    protected Set<DnsZone> handleInternal(Request request, Response response) {
        return Set.of();
    }
}
