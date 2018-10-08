package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

public class CloudbreakClusterTestConfiguration extends CloudbreakTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakClusterTestConfiguration.class);

    private static final int MAX_RETRY = 360;

    private static final int SLEEP_TIME = 10;

    @BeforeClass
    public void cleanUpClusterBeforeTestClass() throws Exception {
        String clusterName = getTestParameter().get("clusterName");

        LOGGER.info("Delete cluster ::: [{}]", clusterName);
        try {
            given(CloudbreakClient.created());
            given(Stack.request().withName(clusterName));
            when(Stack.delete(StackAction::deleteWithForce));
        } catch (WebApplicationException webappExp) {
            try (Response response = webappExp.getResponse()) {
                String exceptionMessage = response.readEntity(String.class);
                String errorMessage = exceptionMessage.substring(exceptionMessage.lastIndexOf(':') + 1);
                LOGGER.info("Cloudbreak Delete Cluster Exception message ::: " + errorMessage);
            } finally {
                LOGGER.info("Cloudbreak Delete Cluster have been done.");
            }
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
