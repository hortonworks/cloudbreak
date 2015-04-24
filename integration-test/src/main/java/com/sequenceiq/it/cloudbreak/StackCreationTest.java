package com.sequenceiq.it.cloudbreak;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.it.IntegrationTestContext;

public class StackCreationTest extends AbstractCloudbreakIntegrationTest {
    @BeforeMethod
    public void setContextParams() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.TEMPLATE_ID, List.class), "Template id is mandatory.");
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID), "Credential id is mandatory.");
    }

    @Test
    @Parameters({ "stackName", "region", "ambariUser", "ambariPassword", "onFailureAction", "threshold", "adjustmentType" })
    public void testStackCreation(@Optional("testing1") String stackName, @Optional("EUROPE_WEST1_B") String region, @Optional("admin") String ambariUser,
            @Optional("admin") String ambariPassword, @Optional("DO_NOTHING") String onFailureAction, @Optional("4") Long threshold,
            @Optional("EXACT") String adjustmentType) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        List<InstanceGroup> instanceGroups = itContext.getContextParam(CloudbreakITContextConstants.TEMPLATE_ID, List.class);
        Map<String, Object> igMap = new HashMap<>();
        for (InstanceGroup ig : instanceGroups) {
            igMap.put(ig.getName(), ig);
        }
        String credentialId = itContext.getContextParam(CloudbreakITContextConstants.CREDENTIAL_ID);
        // WHEN
        String stackId = getClient().postStack(stackName, ambariUser, ambariPassword, credentialId, region, false, igMap, onFailureAction, threshold,
                adjustmentType, null, Collections.<String, String>emptyMap());
        // THEN
        Assert.assertNotNull(stackId);
        itContext.putCleanUpParam(CloudbreakITContextConstants.STACK_ID, stackId);
        CloudbreakUtil.waitForStackStatus(itContext, stackId, "AVAILABLE");
        itContext.putContextParam(CloudbreakITContextConstants.STACK_ID, stackId);
    }
}
