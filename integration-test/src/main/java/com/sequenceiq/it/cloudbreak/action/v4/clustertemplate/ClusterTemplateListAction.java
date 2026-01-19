package com.sequenceiq.it.cloudbreak.action.v4.clustertemplate;

import java.time.Duration;
import java.util.Collection;

import jakarta.ws.rs.ServerErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.responses.ClusterTemplateViewV4Response;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.clustertemplate.ClusterTemplateTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.ClusterTemplateUtil;

public class ClusterTemplateListAction implements Action<ClusterTemplateTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTemplateListAction.class);

    @Override
    public ClusterTemplateTestDto action(TestContext testContext, ClusterTemplateTestDto testDto, CloudbreakClient client) throws Exception {
        Log.when(LOGGER, " ClusterTemplateEntity list request");
        // because the default list is too slow, we need to check until finished, approx 3 mins
        boolean run = true;
        int maxRetry = 10;
        int count = 0;
        int waitInSec = 20;
        long start = System.currentTimeMillis();
        boolean success = false;
        while (run && count < maxRetry) {
            try {
                Collection<ClusterTemplateViewV4Response> responses = client.getDefaultClient(testContext)
                        .clusterTemplateV4EndPoint()
                        .list(client.getWorkspaceId()).getResponses();
                testDto.setResponses(ClusterTemplateUtil.getResponseFromViews(responses));
                Log.whenJson(LOGGER, " ClusterTemplateEntity list successfully:\n", responses);
                run = false;
                success = true;
            } catch (ServerErrorException e) {
                LOGGER.info("Server exception occurred: {}", e.getMessage(), e);
                Thread.sleep(Duration.ofSeconds(waitInSec).toMillis());
            }
            count++;
        }
        long duration = System.currentTimeMillis() - start;
        if (!success) {
            throw new TestFailException("Listing of cluster template timed out, cannot fetched in " + duration);
        }
        LOGGER.info("Cluster template listed in {}ms", duration);
        return testDto;
    }
}
