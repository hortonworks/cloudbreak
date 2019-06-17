package com.sequenceiq.it.cloudbreak;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.environment.client.EnvironmentServiceEndpoints;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

public class EnvironmentServiceClient extends MicroserviceClient {
    public static final String ENVIRONMENTSERVICE_CLIENT = "ENVIRONMENTSERVICE_CLIENT";

    public static final String ENVIRONMENTSERVICE_SERVER_ROOT = "ENVIRONMENTSERVICE_SERVER_ROOT";

    private static String crn;

    private EnvironmentServiceEndpoints environmentServiceEndpoint;

    EnvironmentServiceClient(String newId) {
        super(newId);
    }

    EnvironmentServiceClient() {
        this(ENVIRONMENTSERVICE_CLIENT);
    }

    public static Function<IntegrationTestContext, EnvironmentServiceClient> getTestContextEnvironmentServiceClient(String key) {
        return testContext -> testContext.getContextParam(key, EnvironmentServiceClient.class);
    }

    public static Function<IntegrationTestContext, EnvironmentServiceClient> getTestContextEnvironmentServiceClient() {
        return getTestContextEnvironmentServiceClient(ENVIRONMENTSERVICE_CLIENT);
    }

    public static synchronized EnvironmentServiceClient createProxyEnvironmentServiceClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        EnvironmentServiceClient clientEntity = new EnvironmentServiceClient();
        clientEntity.environmentServiceEndpoint = new ProxyEnvironmentServiceClient(
                testParameter.get(ENVIRONMENTSERVICE_SERVER_ROOT),
                new ConfigKey(false, true, true)).withCrn(cloudbreakUser.getToken());
        return clientEntity;
    }

    public EnvironmentServiceEndpoints getEnvironmentServiceClient() {
        return environmentServiceEndpoint;
    }

    public void setEnvironmentServiceClient(EnvironmentServiceEndpoints environmentServiceEndpoint) {
        this.environmentServiceEndpoint = environmentServiceEndpoint;
    }
}
