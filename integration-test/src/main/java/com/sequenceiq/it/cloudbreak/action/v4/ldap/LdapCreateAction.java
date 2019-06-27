package com.sequenceiq.it.cloudbreak.action.v4.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class LdapCreateAction implements Action<LdapTestDto, FreeIPAClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapCreateAction.class);

    @Override
    public LdapTestDto action(TestContext testContext, LdapTestDto testDto, FreeIPAClient client) throws Exception {
        Log.log(LOGGER, String.format(" Name: %s", testDto.getRequest().getName()));
        Log.logJSON(LOGGER, " Ldap post request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getFreeIpaClient()
                        .getLdapConfigV1Endpoint()
                        .create(testDto.getRequest()));
        Log.logJSON(LOGGER, " Ldap was created successfully:\n", testDto.getResponse());
        Log.log(LOGGER, String.format(" Environment: %s", testDto.getResponse().getEnvironmentCrn()));
        return testDto;
    }
}
