package com.sequenceiq.mock.legacy.freeipa.response;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.DnsRecord;

@Component
public class DnsRecordFindResponse extends AbstractFreeIpaResponse<Set<DnsRecord>> {
    @Override
    public String method() {
        return "dnsrecord_find";
    }

    @Override
    protected Set<DnsRecord> handleInternal(String body) {
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setIdnsname("admin");
        return Set.of(dnsRecord);
    }
}
