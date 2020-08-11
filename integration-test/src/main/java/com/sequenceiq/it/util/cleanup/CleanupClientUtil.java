package com.sequenceiq.it.util.cleanup;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.CloudbreakApiKeyClient;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.client.EnvironmentClient;
import com.sequenceiq.environment.client.EnvironmentServiceApiKeyClient;
import com.sequenceiq.sdx.client.SdxClient;
import com.sequenceiq.sdx.client.SdxServiceApiKeyClient;

@Service
public class CleanupClientUtil {

    private EnvironmentClient environmentClient;

    private SdxClient sdxClient;

    private CloudbreakClient cloudbreakClient;

    @Value("${integrationtest.cloudbreak.server}")
    private String server;

    @Value("${integrationtest.cloudbreak.server.contextPath:/cb}")
    private String cbRootContextPath;

    @Value("${integrationtest.environment.server.contextPath:/environmentservice}")
    private String envRootContextPath;

    @Value("${integrationtest.sdx.server.contextPath:/dl}")
    private String sdxRootContextPath;

    @Value("${integrationtest.user.accesskey:}")
    private String accesskey;

    @Value("${integrationtest.user.secretkey:}")
    private String secretkey;

    public CleanupClientUtil() {
    }

    public CloudbreakClient createCloudbreakClient() {
        cloudbreakClient = new CloudbreakApiKeyClient(
                server + cbRootContextPath,
                new ConfigKey(false, true, true))
                .withKeys(accesskey, secretkey);
        return cloudbreakClient;
    }

    public void setCloudbreakClient(CloudbreakClient cloudbreakClient) {
        this.cloudbreakClient = cloudbreakClient;
    }

    public CloudbreakClient getCloudbreakClient() {
        return cloudbreakClient;
    }

    public EnvironmentClient createEnvironmentClient() {
        environmentClient = new EnvironmentServiceApiKeyClient(
                server + envRootContextPath,
                new ConfigKey(false, true, true))
                .withKeys(accesskey, secretkey);
        return environmentClient;
    }

    public void setEnvironmentClient(EnvironmentClient environmentClient) {
        this.environmentClient = environmentClient;
    }

    public EnvironmentClient getEnvironmentClient() {
        return environmentClient;
    }

    public SdxClient createSdxClient() {
        sdxClient = new SdxServiceApiKeyClient(
                server + sdxRootContextPath,
                new ConfigKey(false, true, true))
                .withKeys(accesskey, secretkey);
        return sdxClient;
    }

    public void setSdxClient(SdxClient sdxClient) {
        this.sdxClient = sdxClient;
    }

    public SdxClient getSdxClient() {
        return sdxClient;
    }
}
