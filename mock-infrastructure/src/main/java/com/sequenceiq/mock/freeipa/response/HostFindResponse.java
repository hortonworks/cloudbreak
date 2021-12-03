package com.sequenceiq.mock.freeipa.response;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.Host;

@Component
public class HostFindResponse extends AbstractFreeIpaResponse<Set<Host>> {
    @Override
    public String method() {
        return "host_find";
    }

    @Override
    protected Set<Host> handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        Host host = new Host();
        host.setFqdn("dummy");
        return Set.of(host);
    }
}
