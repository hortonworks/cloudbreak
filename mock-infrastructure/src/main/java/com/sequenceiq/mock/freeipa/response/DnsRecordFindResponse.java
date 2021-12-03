package com.sequenceiq.mock.freeipa.response;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.DnsRecord;

@Component
public class DnsRecordFindResponse extends AbstractFreeIpaResponse<Set<DnsRecord>> {
    @Override
    public String method() {
        return "dnsrecord_find";
    }

    @Override
    protected Set<DnsRecord> handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setIdnsname("admin");
        return Set.of(dnsRecord);
    }
}
