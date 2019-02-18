package com.sequenceiq.it.cloudbreak.newway.action.ldap;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;

public class LdapConfigCreateIfNotExistsAction implements Action<LdapConfigTestDto> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigCreateIfNotExistsAction.class);

    @Override
    public LdapConfigTestDto action(TestContext testContext, LdapConfigTestDto entity, CloudbreakClient client) throws Exception {
        LOGGER.info("Create LdapConfig with name: {}", entity.getRequest().getName());
        try {
            entity.setResponse(
                    client.getCloudbreakClient().ldapConfigV4Endpoint().post(client.getWorkspaceId(), entity.getRequest())
            );
            logJSON(LOGGER, "LdapConfig created successfully: ", entity.getRequest());
        } catch (Exception e) {
            LOGGER.info("Cannot create LdapConfig, fetch existed one: {}", entity.getRequest().getName());
            entity.setResponse(
                    client.getCloudbreakClient().ldapConfigV4Endpoint()
                            .get(client.getWorkspaceId(), entity.getRequest().getName()));
        }
        if (entity.getResponse() == null) {
            throw new IllegalStateException("LdapConfig could not be created.");
        }
        return entity;
    }
}
