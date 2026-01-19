package com.sequenceiq.it.cloudbreak.action.v4.ldap;

import jakarta.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;

public class LdapDeleteAction implements Action<LdapTestDto, FreeIpaClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapDeleteAction.class);

    @Override
    public LdapTestDto action(TestContext testContext, LdapTestDto testDto, FreeIpaClient client) throws Exception {
        Log.when(LOGGER, " LDAP config delete request: " + testDto.getName());
        client.getDefaultClient(testContext)
            .getLdapConfigV1Endpoint()
            .delete(testDto.getName());
        try {
            testDto.setResponse(
                    client.getDefaultClient(testContext)
                            .getLdapConfigV1Endpoint()
                            .describe(testDto.getName()));
        } catch (NotFoundException e) {
            Log.when(LOGGER, String.format(" LDAP config was deleted successfully for environment %s", testDto.getName()));
        }
        return testDto;
    }
}
