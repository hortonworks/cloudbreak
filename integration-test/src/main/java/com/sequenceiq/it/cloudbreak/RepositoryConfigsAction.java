package com.sequenceiq.it.cloudbreak;

import com.sequenceiq.it.IntegrationTestContext;

public class RepositoryConfigsAction {
    private RepositoryConfigsAction() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) {
        RepositoryConfigs repositoryConfigsEntity = (RepositoryConfigs) entity;
        CloudbreakClient client;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);

        repositoryConfigsEntity.setResponse(
                client.getDefaultClient()
                        .utilV4Endpoint()
                        .repositoryConfigValidationRequest(repositoryConfigsEntity.getRequest()));
    }
}
