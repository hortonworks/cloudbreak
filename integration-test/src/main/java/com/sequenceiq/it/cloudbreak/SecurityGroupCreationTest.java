package com.sequenceiq.it.cloudbreak;

import java.util.Arrays;

import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.model.SecurityGroupJson;
import com.sequenceiq.cloudbreak.api.model.SecurityRuleJson;

public class SecurityGroupCreationTest extends AbstractCloudbreakIntegrationTest {

    @Test
    @Parameters({ "name", "ports" })
    public void testSecurityGroupCreation(@Optional("it-restricted-ambari") String name, @Optional("22,443,9443,8080") String ports) throws Exception {
        // GIVEN
        // WHEN
        SecurityGroupJson securityGroupJson = new SecurityGroupJson();
        securityGroupJson.setDescription("Security group created by IT");
        securityGroupJson.setName(name);
        SecurityRuleJson securityRuleJson = new SecurityRuleJson();
        securityRuleJson.setProtocol("tcp");
        securityRuleJson.setSubnet("0.0.0.0/0");
        securityRuleJson.setPorts(ports);
        securityGroupJson.setSecurityRules(Arrays.asList(securityRuleJson));

        String id = getCloudbreakClient().securityGroupEndpoint().postPrivate(securityGroupJson).getId().toString();
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID, id, true);
    }

}
