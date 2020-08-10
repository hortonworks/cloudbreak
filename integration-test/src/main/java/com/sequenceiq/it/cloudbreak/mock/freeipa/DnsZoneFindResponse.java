package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.DnsZoneList;

import spark.Request;
import spark.Response;

@Component
public class DnsZoneFindResponse extends AbstractFreeIpaResponse<Set<DnsZoneList>> {
    @Override
    public String method() {
        return "dnszone_find";
    }

    @Override
    protected Set<DnsZoneList> handleInternal(Request request, Response response) {
        return Set.of();
    }
}
