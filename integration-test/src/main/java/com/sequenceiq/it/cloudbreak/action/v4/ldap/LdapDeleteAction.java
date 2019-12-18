package com.sequenceiq.it.cloudbreak.action.v4.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class LdapDeleteAction implements Action<LdapTestDto, FreeIPAClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapDeleteAction.class);

    @Override
    public LdapTestDto action(TestContext testContext, LdapTestDto testDto, FreeIPAClient client) throws Exception {
        Log.when(LOGGER, " LDAP config delete request: " + testDto.getName());
        client.getFreeIpaClient()
            .getLdapConfigV1Endpoint()
            .delete(testDto.getName());
        Log.when(LOGGER, String.format(" LDAP config was deleted successfully for environment %s", testDto.getName()));
        return testDto;
    }
}
