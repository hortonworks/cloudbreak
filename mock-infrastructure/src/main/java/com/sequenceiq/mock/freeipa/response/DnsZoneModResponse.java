package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.DnsZone;

@Component
public class DnsZoneModResponse extends AbstractFreeIpaResponse<DnsZone> {
    @Override
    public String method() {
        return "dnszone_mod";
    }

    @Override
    protected DnsZone handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        DnsZone dnsZone = new DnsZone();
        dnsZone.setIdnsname("");
        return dnsZone;
    }
}
