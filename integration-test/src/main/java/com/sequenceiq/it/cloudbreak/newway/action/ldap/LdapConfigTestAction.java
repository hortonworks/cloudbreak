package com.sequenceiq.it.cloudbreak.newway.action.ldap;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.ldap.LdapConfigTestDto;

public class LdapConfigTestAction {

    private LdapConfigTestAction() {
    }

    public static Action<LdapConfigTestDto> postV4() {
        return new LdapConfigPostAction();
    }

    public static Action<LdapConfigTestDto> deleteV4() {
        return new LdapConfigDeleteAction();
    }

    public static LdapConfigTestDto getByName(TestContext testContext, LdapConfigTestDto entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().ldapConfigV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }
}