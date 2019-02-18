package com.sequenceiq.it.cloudbreak.newway.action.ldap;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static java.lang.String.format;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;

public class LdapConfigDeleteAction implements Action<LdapConfigTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigDeleteAction.class);

    @Override
    public LdapConfigTestDto action(TestContext testContext, LdapConfigTestDto entity, CloudbreakClient client) throws Exception {
        log(LOGGER, format(" Name: %s", entity.getRequest().getName()));
        logJSON(LOGGER, " LDAP config delete request:\n", entity.getRequest());
        entity.setResponse(
                client.getCloudbreakClient()
                        .ldapConfigV4Endpoint()
                        .delete(client.getWorkspaceId(), entity.getName()));
        logJSON(LOGGER, " LDAP config was deleted successfully:\n", entity.getResponse());
        log(LOGGER, format(" ID: %s", entity.getResponse().getId()));
        return entity;
    }

}