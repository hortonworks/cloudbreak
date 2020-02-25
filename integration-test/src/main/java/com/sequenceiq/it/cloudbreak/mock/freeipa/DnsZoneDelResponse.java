package com.sequenceiq.it.cloudbreak.mock.freeipa;

import org.springframework.stereotype.Component;

import spark.Request;
import spark.Response;

@Component
public class DnsZoneDelResponse extends AbstractFreeIpaResponse<Object> {
    @Override
    public String method() {
        return "dnszone_del";
    }

    @Override
    protected Object handleInternal(Request request, Response response) {
        return "";
    }
}
