package com.sequenceiq.it.cloudbreak.preflight;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.config.CcmProperties;

@Component
public class CcmConnectivityCheck implements PreflightCheck {

    private static final int CONNECT_TIMEOUT_MS = 5000;

    @Inject
    private CcmProperties ccmProperties;

    @Inject
    private CloudProviderEndpointResolver endpointResolver;

    @Override
    public PreflightCheckResult check() {
        if (CloudPlatform.MOCK == endpointResolver.activeCloudPlatform()) {
            return PreflightCheckResult.skip(getName(), "MOCK provider, no real CCM dependency");
        }
        if (StringUtils.isBlank(ccmProperties.getHost())) {
            return PreflightCheckResult.skip(getName(), "integrationtest.ccm.host not configured");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ccmProperties.getHost(), ccmProperties.getPort()), CONNECT_TIMEOUT_MS);
            return PreflightCheckResult.pass(getName(), "connected to " + ccmProperties.getHost() + ":" + ccmProperties.getPort());
        } catch (IOException e) {
            return PreflightCheckResult.fail(getName(),
                    "could not connect to " + ccmProperties.getHost() + ":" + ccmProperties.getPort() + ": " + e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "ccm-connectivity";
    }
}
