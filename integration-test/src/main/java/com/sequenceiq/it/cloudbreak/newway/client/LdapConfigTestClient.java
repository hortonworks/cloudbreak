package com.sequenceiq.it.cloudbreak.newway.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.ldap.LdapConfigCreateIfNotExistsAction;
import com.sequenceiq.it.cloudbreak.newway.action.ldap.LdapConfigDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.ldap.LdapConfigPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;

@Service
public class LdapConfigTestClient {

    public Action<LdapConfigTestDto> post() {
        return new LdapConfigPostAction();
    }

    public Action<LdapConfigTestDto> delete() {
        return new LdapConfigDeleteAction();
    }

    public Action<LdapConfigTestDto> createIfNotExists() {
        return new LdapConfigCreateIfNotExistsAction();
    }

    public LdapConfigTestDto getByName(TestContext testContext, LdapConfigTestDto entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().ldapConfigV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }
}