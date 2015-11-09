package com.sequenceiq.it.cloudbreak;

import static java.util.Collections.singletonMap;

import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

public class SecurityGroupCreationTest extends AbstractCloudbreakIntegrationTest {

    @Test
    @Parameters({ "name", "ports" })
    public void testGcpTemplateCreation(@Optional("restricted-ambari") String name, @Optional("22,443,8080") String ports) throws Exception {
        // GIVEN
        // WHEN
        String id = getClient().postSecurityGroup(name, "Security group created by IT", singletonMap("0.0.0.0/0", ports), null, false);
        // THEN
        Assert.assertNotNull(id);
        getItContext().putContextParam(CloudbreakITContextConstants.SECURITY_GROUP_ID, id, true);
    }

}
