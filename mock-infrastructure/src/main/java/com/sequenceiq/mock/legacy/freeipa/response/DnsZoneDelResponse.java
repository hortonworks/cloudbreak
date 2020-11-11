package com.sequenceiq.mock.legacy.freeipa.response;

import org.springframework.stereotype.Component;

@Component
public class DnsZoneDelResponse extends AbstractFreeIpaResponse<Object> {
    @Override
    public String method() {
        return "dnszone_del";
    }

    @Override
    protected Object handleInternal(String body) {
        return "";
    }
}
