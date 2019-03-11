package com.sequenceiq.it.cloudbreak.newway.action.v4.ldap;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapTestDto;

public class LdapGetAction implements Action<LdapTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapGetAction.class);

    @Override
    public LdapTestDto action(TestContext testContext, LdapTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, " Ldap get:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .ldapConfigV4Endpoint()
                        .get(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, " Ldap get was successfully:\n", entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));

        return entity;
    }

}