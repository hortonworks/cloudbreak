package com.sequenceiq.it.cloudbreak.newway.action.v4.ldap;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.ldap.LdapTestDto;

public class LdapCreateIfNotExistsAction implements Action<LdapTestDto> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapCreateIfNotExistsAction.class);

    @Override
    public LdapTestDto action(TestContext testContext, LdapTestDto testDto, CloudbreakClient client) throws Exception {
        LOGGER.info("Create LdapConfig with name: {}", testDto.getRequest().getName());
        try {
            testDto.setResponse(
                    client.getCloudbreakClient().ldapConfigV4Endpoint().post(client.getWorkspaceId(), testDto.getRequest())
            );
            logJSON(LOGGER, "LdapConfig created successfully: ", testDto.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create LdapConfig, fetch existed one: {}", testDto.getRequest().getName());
            testDto.setResponse(
                    client.getCloudbreakClient().ldapConfigV4Endpoint()
                            .get(client.getWorkspaceId(), testDto.getRequest().getName()));
        }
        if (testDto.getResponse() == null) {
            throw new IllegalStateException("LdapConfig could not be created.");
        }
        return testDto;
    }
}
