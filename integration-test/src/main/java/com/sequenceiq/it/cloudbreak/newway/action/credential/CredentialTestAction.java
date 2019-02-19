package com.sequenceiq.it.cloudbreak.newway.action.credential;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.it.cloudbreak.exception.ProxyMethodInvocationException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;

public class CredentialTestAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialTestAction.class);

    private CredentialTestAction() {

    }

    public static CredentialTestDto createIfNotExist(TestContext testContext, CredentialTestDto entity, CloudbreakClient client) throws Exception {
        LOGGER.info("Create Credential with name: {}", entity.getRequest().getName());
        try {
            entity.setResponse(
                    client.getCloudbreakClient().credentialV4Endpoint().post(client.getWorkspaceId(), entity.getRequest())
            );
            logJSON(LOGGER, "Credential created successfully: ", entity.getRequest());
        } catch (ProxyMethodInvocationException e) {
            LOGGER.info("Cannot create Credential, fetch existed one: {}", entity.getRequest().getName());
            entity.setResponse(
                    client.getCloudbreakClient().credentialV4Endpoint()
                            .get(client.getWorkspaceId(), entity.getRequest().getName()));
        }
        if (entity.getResponse() == null) {
            throw new IllegalStateException("Credential could not be created.");
        }
        return entity;
    }

    public static CredentialTestDto list(TestContext testContext, CredentialTestDto entity, CloudbreakClient client) throws Exception {
        Collection<CredentialV4Response> responses = client.getCloudbreakClient()
                .credentialV4Endpoint()
                .list(client.getWorkspaceId())
                .getResponses();
        entity.setResponses(responses.stream().collect(Collectors.toSet()));
        logJSON(LOGGER, " Credential listed successfully:\n", entity.getResponses());
        return entity;
    }

    public static CredentialTestDto delete(TestContext testContext, CredentialTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, " Credential delete request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .credentialV4Endpoint() 
                        .delete(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, " Credential deleted successfully:\n", entity.getResponse());
        return entity;
    }

    public static CredentialTestDto create(TestContext testContext, CredentialTestDto entity, CloudbreakClient client) throws Exception {
        logJSON(LOGGER, " Credential create request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .credentialV4Endpoint()
                        .post(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, " Credential created successfully:\n", entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }

    public static CredentialTestDto modify(TestContext testContext, CredentialTestDto entity, CloudbreakClient client) throws Exception {
        logJSON(LOGGER, " Credential modify request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .credentialV4Endpoint()
                        .put(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, " Credential modified successfully:\n", entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }
}
