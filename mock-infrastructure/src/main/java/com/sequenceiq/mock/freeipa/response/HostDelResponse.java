package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.Host;

@Component
public class HostDelResponse extends AbstractFreeIpaResponse<Host> {
    @Override
    public String method() {
        return "host_del";
    }

    @Override
    protected Host handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        Host host = new Host();
        host.setFqdn("dummy");
        return host;
    }
}
