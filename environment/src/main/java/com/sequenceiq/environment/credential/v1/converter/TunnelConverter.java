package com.sequenceiq.environment.credential.v1.converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.base.Tunnel;

@Component
public class TunnelConverter {

    @Value("${environment.tunnel.default}")
    private Tunnel defaultTunnel;

    public Tunnel convert(Tunnel tunnel) {
        if (tunnel == null) {
            return defaultTunnel;
        }
        return tunnel;
    }
}
