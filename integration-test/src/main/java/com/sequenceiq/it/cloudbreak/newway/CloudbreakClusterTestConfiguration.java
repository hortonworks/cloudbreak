package com.sequenceiq.it.cloudbreak.newway;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.ForbiddenException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;

import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;

public class CloudbreakClusterTestConfiguration extends CloudbreakTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakClusterTestConfiguration.class);

    private static final int MAX_RETRY = 360;

    private static final int SLEEP_TIME = 10;

    @BeforeClass
    public void cleanUpClusterBeforeTestClass() throws Exception {
        String clusterName = getTestParameter().get("clusterName");

        given(CloudbreakClient.isCreated());
        given(Stack.request().withName(clusterName));
        try {
            when(Stack.delete(StackAction::deleteWithForce));
        } catch (Exception e) {
            LOGGER.info("Could not delete stack", e);
        }

        int retryCount = 0;
        Long workspaceId = getItContext().getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);

        while (getRequestFromName(workspaceId, clusterName) != null && retryCount++ < MAX_RETRY) {
            TimeUnit.SECONDS.sleep(SLEEP_TIME);
        }
    }

    private StackV2Request getRequestFromName(Long workspaceId, String clusterName) {
        StackV2Request result;
        try {
            result = CloudbreakClient.getTestContextCloudbreakClient().apply(getItContext()).getCloudbreakClient()
                    .stackV3Endpoint().getRequestfromName(workspaceId, clusterName);
        } catch (ForbiddenException ex) {
            result = null;
        }
        return result;
    }
}
