package com.sequenceiq.it.cloudbreak.newway.action;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.LdapConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class LdapConfigPostAction implements ActionV2<LdapConfigEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigPostAction.class);

    @Override
    public LdapConfigEntity action(TestContext testContext, LdapConfigEntity entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, format(" Ldap post request:%n"), entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .ldapConfigV4Endpoint()
                        .post(client.getWorkspaceId(), entity.getRequest()));
        logJSON(LOGGER, format(" Ldap created  successfully:%n"), entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }

}