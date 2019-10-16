package com.sequenceiq.environment.credential.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.base.Tunnel;

@Component
public class TunnelConverter {

    public Tunnel convert(Tunnel tunnelRequest) {
        if (tunnelRequest == null) {
            tunnelRequest = Tunnel.DIRECT;
        }
        return tunnelRequest;
    }
}
