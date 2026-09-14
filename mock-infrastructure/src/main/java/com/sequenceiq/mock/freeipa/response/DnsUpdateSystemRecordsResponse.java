package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;

@Component
public class DnsUpdateSystemRecordsResponse extends AbstractFreeIpaResponse<Object> {
    @Override
    public String method() {
        return "dns_update_system_records";
    }

    @Override
    protected Object handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        return "";
    }
}
