package com.sequenceiq.mock.freeipa.response;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.DnsZone;

@Component
public class DnsZoneFindResponse extends AbstractFreeIpaResponse<Set<DnsZone>> {
    @Override
    public String method() {
        return "dnszone_find";
    }

    @Override
    protected Set<DnsZone> handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        return Set.of();
    }
}
