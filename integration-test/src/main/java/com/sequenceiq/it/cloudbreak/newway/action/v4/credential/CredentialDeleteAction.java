package com.sequenceiq.it.cloudbreak.newway.action.v4.credential;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.credential.CredentialTestDto;

public class CredentialDeleteAction implements Action<CredentialTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialDeleteAction.class);

    @Override
    public CredentialTestDto action(TestContext testContext, CredentialTestDto entity, CloudbreakClient cloudbreakClient) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, " Credential delete request:\n", entity.getRequest());
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient()
                        .credentialV4Endpoint()
                        .delete(cloudbreakClient.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, " Credential deleted successfully:\n", entity.getResponse());
        return entity;
    }
}
