package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsConfig;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

public class UmsClient extends MicroserviceClient {

    public static final String UMS_CLIENT = "UMS_CLIENT";

    private GrpcUmsClient umsClient;

    protected UmsClient(String newId) {
        super(newId);
    }

    UmsClient() {
        this(UMS_CLIENT);
    }

    public static Function<IntegrationTestContext, SdxClient> getTestContextSdxClient(String key) {
        return testContext -> testContext.getContextParam(key, SdxClient.class);
    }

    public static Function<IntegrationTestContext, SdxClient> getTestContextSdxClient() {
        return getTestContextSdxClient(UMS_CLIENT);
    }

    public static synchronized UmsClient createProxyUmsClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        UmsClient clientEntity = new UmsClient();
        UmsConfig config = UmsConfig.createConfig("ums.thunderhead-dev.cloudera.com", 8982);
        UmsClientConfig clientConfig = new UmsClientConfig();
        clientEntity.umsClient = GrpcUmsClient.createClient(config, clientConfig);
        return clientEntity;
    }

    public GrpcUmsClient getUmsClient() {
        return umsClient;
    }
}
