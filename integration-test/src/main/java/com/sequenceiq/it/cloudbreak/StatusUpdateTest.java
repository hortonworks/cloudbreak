package com.sequenceiq.it.cloudbreak;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.type.StatusRequest;
import com.sequenceiq.cloudbreak.model.UpdateClusterJson;
import com.sequenceiq.cloudbreak.model.UpdateStackJson;
import com.sequenceiq.it.IntegrationTestContext;

public class StatusUpdateTest extends AbstractCloudbreakIntegrationTest {
    private static final String STOPPED = "STOPPED";
    private static final String STARTED = "STARTED";

    @BeforeMethod
    public void setContextParameters() {
        IntegrationTestContext itContext = getItContext();
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.");
    }

    @Test
    @Parameters({ "newStatus" })
    public void testStatusUpdate(@Optional(STOPPED) String newStatus) throws Exception {
        // GIVEN
        IntegrationTestContext itContext = getItContext();
        String stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID);
        Integer stackIntId = Integer.valueOf(stackId);
        // WHEN
        if (newStatus.equals(STOPPED)) {
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            updateClusterJson.setStatus(StatusRequest.valueOf(newStatus));
            getClusterEndpoint().put(Long.valueOf(stackIntId), updateClusterJson);
            CloudbreakUtil.waitAndCheckClusterStatus(getItContext(), stackId, STOPPED);
            UpdateStackJson updateStackJson = new UpdateStackJson();
            updateStackJson.setStatus(StatusRequest.valueOf(newStatus));
            getStackEndpoint().put(Long.valueOf(stackIntId), updateStackJson);
            getStackEndpoint().put(Long.valueOf(stackIntId), updateStackJson);
            CloudbreakUtil.waitAndCheckStackStatus(getItContext(), stackId, STOPPED);
        } else {
            UpdateStackJson updateStackJson = new UpdateStackJson();
            updateStackJson.setStatus(StatusRequest.valueOf(newStatus));
            getStackEndpoint().put(Long.valueOf(stackIntId), updateStackJson);
            CloudbreakUtil.waitAndCheckStackStatus(getItContext(), stackId, "AVAILABLE");
            UpdateClusterJson updateClusterJson = new UpdateClusterJson();
            updateClusterJson.setStatus(StatusRequest.valueOf(newStatus));
            getClusterEndpoint().put(Long.valueOf(stackIntId), updateClusterJson);
            CloudbreakUtil.waitAndCheckClusterStatus(getItContext(), stackId, "AVAILABLE");
        }
        // THEN
        if (newStatus.equals(STARTED)) {
            CloudbreakUtil.checkClusterAvailability(getStackEndpoint(), stackId, itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID),
                    itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID));
        } else if (newStatus.equals(STOPPED)) {
            CloudbreakUtil.checkClusterStopped(getStackEndpoint(), stackId, itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID),
                    itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID));
        }
    }
}
