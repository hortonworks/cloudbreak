package com.sequenceiq.it.cloudbreak.newway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.mock.MockCredentialV4Parameters;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class PostCredentialWithNameFromMockStrategy implements Strategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostCredentialWithNameFromMockStrategy.class);

    @Override
    public void doAction(IntegrationTestContext integrationTestContext, Entity entity) {
        CredentialEntity credentialEntity = (CredentialEntity) entity;
        CloudbreakClient client;
        Mock mock;
        client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT,
                CloudbreakClient.class);
        mock = Mock.getTestContextMock().apply(integrationTestContext);

        int credentialPort = mock.getPort();
        MockCredentialV4Parameters credentialParameters = new MockCredentialV4Parameters();
        credentialParameters.setMockEndpoint(mock.getEndpoint());

        credentialEntity = credentialEntity.withName(credentialEntity.getName() + credentialPort).withMockParameters(credentialParameters);
        Log.log(" post "
                .concat(credentialEntity.getName())
                .concat(" private credential. "));
        try {
            credentialEntity.setResponse(
                    client.getCloudbreakClient()
                            .credentialV4Endpoint()
                            .post(1L, credentialEntity.getRequest()));
        } catch (Exception e) {
            LOGGER.info("Creation of credential has failed, using existing if it is possible", e);
        }
        mock.setCredentialName(credentialEntity.getName());
    }
}
