package com.sequenceiq.it.cloudbreak.mock.freeipa;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.Host;

import spark.Request;
import spark.Response;

@Component
public class HostDelResponse extends AbstractFreeIpaResponse<Host> {
    @Override
    public String method() {
        return "host_del";
    }

    @Override
    protected Host handleInternal(Request request, Response response) {
        Host host = new Host();
        host.setFqdn("dummy");
        return host;
    }
}
