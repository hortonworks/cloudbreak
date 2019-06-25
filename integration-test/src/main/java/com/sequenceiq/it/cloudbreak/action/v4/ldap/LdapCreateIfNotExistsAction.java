package com.sequenceiq.it.cloudbreak.action.v4.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.log.Log;

public class LdapCreateIfNotExistsAction implements Action<LdapTestDto, FreeIPAClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapCreateIfNotExistsAction.class);

    @Override
    public LdapTestDto action(TestContext testContext, LdapTestDto testDto, FreeIPAClient client) throws Exception {
        LOGGER.info("Create LdapConfig for environment: {}", testDto.getRequest().getEnvironmentCrn());
        try {
            testDto.setResponse(
                    client.getFreeIpaClient().getLdapConfigV1Endpoint().create(testDto.getRequest())
            );
            Log.logJSON(LOGGER, "LdapConfig created successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create LdapConfig, fetch existed one: {}", testDto.getRequest().getName());
            testDto.setResponse(
                    client.getFreeIpaClient().getLdapConfigV1Endpoint()
                            .describe(testDto.getRequest().getEnvironmentCrn()));
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("LdapConfig could not be created.");
        }
        return testDto;
    }
}
