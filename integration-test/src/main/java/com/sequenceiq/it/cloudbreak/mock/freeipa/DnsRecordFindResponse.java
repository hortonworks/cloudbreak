package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.DnsRecord;

import spark.Request;
import spark.Response;

@Component
public class DnsRecordFindResponse extends AbstractFreeIpaResponse<Set<DnsRecord>> {
    @Override
    public String method() {
        return "dnsrecord_find";
    }

    @Override
    protected Set<DnsRecord> handleInternal(Request request, Response response) {
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setIdnsname("admin");
        return Set.of(dnsRecord);
    }
}
