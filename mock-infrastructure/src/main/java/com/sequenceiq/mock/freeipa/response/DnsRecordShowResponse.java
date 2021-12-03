package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.DnsRecord;

@Component
public class DnsRecordShowResponse extends AbstractFreeIpaResponse<DnsRecord> {
    @Override
    public String method() {
        return "dnsrecord_show";
    }

    @Override
    protected DnsRecord handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        DnsRecord dnsRecord = new DnsRecord();
        dnsRecord.setIdnsname("localhost");
        return dnsRecord;
    }
}
