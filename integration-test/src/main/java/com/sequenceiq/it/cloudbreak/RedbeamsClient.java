package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.cloudbreak.client.ConfigKey;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.redbeams.client.RedbeamsApiKeyClient;

public class RedbeamsClient extends MicroserviceClient {
    public static final String REDBEAMS_CLIENT = "REDBEAMS_CLIENT";

    private com.sequenceiq.redbeams.client.RedbeamsClient endpoints;

    private String environmentCrn;

    private RedbeamsClient() {
        super(REDBEAMS_CLIENT);
    }

    public static synchronized RedbeamsClient createProxyRedbeamsClient(TestParameter testParameter, CloudbreakUser cloudbreakUser) {
        RedbeamsClient clientEntity = new RedbeamsClient();
        clientEntity.setActing(cloudbreakUser);
        clientEntity.endpoints = new RedbeamsApiKeyClient(
                testParameter.get(RedBeamsTest.REDBEAMS_SERVER_ROOT),
                new ConfigKey(false, true, true, TIMEOUT))
                .withKeys(cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey());
        return clientEntity;
    }

    public com.sequenceiq.redbeams.client.RedbeamsClient getEndpoints() {
        return endpoints;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }
}
