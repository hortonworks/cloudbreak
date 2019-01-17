package com.sequenceiq.it.cloudbreak.newway;

import java.io.IOException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.filter.SecurityRulesV4Filter;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class SecurityRulesAction {

    private SecurityRulesAction() {
    }

    public static void getDefaultSecurityRules(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        SecurityRulesEntity securityRulesEntity = (SecurityRulesEntity) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);

        Log.log(" get Security Rules to");
        SecurityRulesV4Filter securityRulesV4Filter = new SecurityRulesV4Filter();
        securityRulesV4Filter.setKnoxEnabled(Boolean.TRUE);
        securityRulesEntity.setResponse(client.getCloudbreakClient().utilV4Endpoint().getDefaultSecurityRules(securityRulesV4Filter));
        Log.logJSON(" get Security Rules response: ", securityRulesEntity.getResponse());
    }
}
