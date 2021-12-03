package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.DnsZone;

@Component
public class DnsZoneAddResponse extends AbstractFreeIpaResponse<DnsZone> {
    @Override
    public String method() {
        return "dnszone_add";
    }

    @Override
    protected DnsZone handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        return new DnsZone();
    }
}
