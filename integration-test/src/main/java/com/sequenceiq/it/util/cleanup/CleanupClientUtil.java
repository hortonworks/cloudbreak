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
        return new CloudbreakApiKeyClient(
                server + cbRootContextPath,
                new ConfigKey(false, true, true))
                .withKeys(accesskey, secretkey);
    }

    public EnvironmentClient createEnvironmentClient() {
        return new EnvironmentServiceApiKeyClient(
                server + envRootContextPath,
                new ConfigKey(false, true, true))
                .withKeys(accesskey, secretkey);
    }

    public SdxClient createSdxClient() {
        return new SdxServiceApiKeyClient(
                server + sdxRootContextPath,
                new ConfigKey(false, true, true))
                .withKeys(accesskey, secretkey);
    }
}
