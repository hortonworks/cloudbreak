package com.sequenceiq.it.cloudbreak.newway.action.v4.credential;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.ProxyMethodInvocationException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;

public class CredentialCreateIfNotExistAction implements Action<CredentialTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialCreateIfNotExistAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        LOGGER.info("Create Credential with name: {}", entity.getRequest().getName());
        try {
            entity.setResponse(
                    cloudbreakClient.getCloudbreakClient().credentialV4Endpoint().post(cloudbreakClient.getWorkspaceId(), entity.getRequest())
            );
            logJSON(LOGGER, "Credential created successfully: ", entity.getRequest());
        } catch (ProxyMethodInvocationException e) {
            LOGGER.info("Cannot create Credential, fetch existed one: {}", entity.getRequest().getName());
            entity.setResponse(
                    cloudbreakClient.getCloudbreakClient().credentialV4Endpoint()
                            .get(cloudbreakClient.getWorkspaceId(), entity.getRequest().getName()));
        }
        if (entity.getResponse() == null) {
            throw new IllegalStateException("Credential could not be created.");
        }
        return entity;    }
}
