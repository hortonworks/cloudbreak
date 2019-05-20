package com.sequenceiq.it.cloudbreak.action.v4.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class LdapGetAction implements Action<LdapTestDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapGetAction.class);

    @Override
    public LdapTestDto action(TestContext testContext, LdapTestDto testDto, CloudbreakClient client) throws Exception {
        Log.log(LOGGER, String.format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, " Ldap get:\n", testDto.getRequest());
        testDto.setResponse(
                client.getCloudbreakClient()
                        .ldapConfigV4Endpoint()
                        .get(client.getWorkspaceId(), testDto.getName()));
        Log.logJSON(LOGGER, " Ldap get was successfully:\n", testDto.getResponse());
        Log.log(LOGGER, String.format(" ID: %s", testDto.getResponse().getId()));

        return testDto;
    }

}