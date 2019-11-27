package com.sequenceiq.environment.credential.v1.converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.common.api.type.Tunnel;

@Component
public class TunnelConverter {

    @Value("${environment.tunnel.default}")
    private Tunnel defaultTunnel;

    public Tunnel convert(Tunnel tunnelRequest) {
        if (tunnelRequest == null) {
            tunnelRequest = defaultTunnel;
        }
        return tunnelRequest;
    }
}
