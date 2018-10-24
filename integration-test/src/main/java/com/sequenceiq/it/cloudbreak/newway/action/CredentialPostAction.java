package com.sequenceiq.it.cloudbreak.newway.action;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CredentialEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

public class CredentialPostAction implements ActionV2<CredentialEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CredentialPostAction.class);

    @Override
    public CredentialEntity action(TestContext testContext, CredentialEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Credential post request:%n"), entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .credentialV3Endpoint()
                        .createInWorkspace(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, format(" Credential created  successfully:%n"), entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }

}