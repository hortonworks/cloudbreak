package com.sequenceiq.it.cloudbreak.action.v4.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class LdapCreateAction implements Action<LdapTestDto, FreeIpaClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapCreateAction.class);

    @Override
    public LdapTestDto action(TestContext testContext, LdapTestDto testDto, FreeIpaClient client) throws Exception {
        Log.whenJson(LOGGER, " Ldap post request:\n", testDto.getRequest());
        testDto.setResponse(
                client.getDefaultClient(testContext)
                        .getLdapConfigV1Endpoint()
                        .create(testDto.getRequest()));
        Log.whenJson(LOGGER, " Ldap was created successfully:\n", testDto.getResponse());
        return testDto;
    }
}
