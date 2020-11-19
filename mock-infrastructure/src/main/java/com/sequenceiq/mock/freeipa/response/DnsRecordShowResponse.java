package com.sequenceiq.mock.freeipa.response;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.DnsRecord;

@Component
public class DnsRecordShowResponse extends AbstractFreeIpaResponse<DnsRecord> {
    @Override
    public String method() {
        return "dnsrecord_show";
    }

    @Override
    protected DnsRecord handleInternal(String body) {
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setIdnsname("localhost");
        return dnsRecord;
    }
}
