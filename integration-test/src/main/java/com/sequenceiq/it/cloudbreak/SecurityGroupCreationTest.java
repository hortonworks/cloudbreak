package com.sequenceiq.it.cloudbreak;

import java.util.Collections;

import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.SecurityGroupRequest;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;

public class SecurityGroupCreationTest extends AbstractCloudbreakIntegrationTest {

    @Test
    @Parameters({ "name", "ports" })
    public void testSecurityGroupCreation(@Optional("it-restricted-ambari") String name, @Optional("22,443,9443,8080") String ports) throws Exception {
        // GIVEN
        // WHEN
        SecurityGroupRequest securityGroupRequest = new SecurityGroupRequest();
        securityGroupRequest.setDescription("Security group created by IT");
        securityGroupRequest.setName(name);
        SecurityRuleRequest securityRuleRequest = new SecurityRuleRequest("");
        securityRuleRequest.setProtocol("tcp");
        securityRuleRequest.setSubnet("0.0.0.0/0");
        securityRuleRequest.setPorts(ports);
        securityGroupRequest.setSecurityRules(Collections.singletonList(securityRuleRequest));

        String id = getCloudbreakClient().securityGroupEndpoint().postPrivate(securityGroupRequest).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID, id, true);
    }

}
