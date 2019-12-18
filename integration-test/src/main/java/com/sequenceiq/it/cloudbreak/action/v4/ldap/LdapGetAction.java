package com.sequenceiq.it.cloudbreak.action.v4.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class LdapGetAction implements Action<LdapTestDto, FreeIPAClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapGetAction.class);

    @Override
    public LdapTestDto action(TestContext testContext, LdapTestDto testDto, FreeIPAClient client) throws Exception {
        Log.when(LOGGER, " Ldap get: " + testDto.getName());
        testDto.setResponse(
                client.getFreeIpaClient()
                        .getLdapConfigV1Endpoint()
                        .describe(testDto.getName()));
        Log.whenJson(LOGGER, " Ldap get was successfully:\n", testDto.getResponse());

        return testDto;
    }
}
