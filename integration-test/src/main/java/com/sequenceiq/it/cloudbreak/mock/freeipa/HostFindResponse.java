package com.sequenceiq.it.cloudbreak.mock.freeipa;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.Host;

import spark.Request;
import spark.Response;

@Component
public class HostFindResponse extends AbstractFreeIpaResponse<Set<Host>> {
    @Override
    public String method() {
        return "host_find";
    }

    @Override
    protected Set<Host> handleInternal(Request request, Response response) {
        Host host = new Host();
        host.setFqdn("dummy");
        return Set.of(host);
    }
}
