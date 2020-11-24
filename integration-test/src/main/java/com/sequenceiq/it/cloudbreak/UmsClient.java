package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsChannelConfig;
import com.sequenceiq.cloudbreak.auth.altus.config.UmsClientConfig;
import com.sequenceiq.it.IntegrationTestContext;

import io.opentracing.Tracer;

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

    public static synchronized UmsClient createProxyUmsClient(Tracer tracer) {
        UmsClient clientEntity = new UmsClient();
        UmsClientConfig clientConfig = new UmsClientConfig();
        clientEntity.umsClient = GrpcUmsClient.createClient(
                UmsChannelConfig.newManagedChannelWrapper("ums.thunderhead-dev.cloudera.com", 8982), clientConfig, tracer);
        return clientEntity;
    }

    public GrpcUmsClient getUmsClient() {
        return umsClient;
    }
}
