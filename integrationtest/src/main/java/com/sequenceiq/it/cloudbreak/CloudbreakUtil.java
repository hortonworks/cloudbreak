package com.sequenceiq.it.cloudbreak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.testng.Assert;

import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.util.RestUtil;

public class CloudbreakUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUtil.class);
    private static final int MAX_RETRY = 180;
    private static final int POLLING_INTERVAL = 10000;

    private CloudbreakUtil() {
    }

    public static void waitForStackStatus(IntegrationTestContext itContext, String stackId, String desiredStatus) {
        waitForStackStatus(itContext, stackId, desiredStatus, "status");
    }

    public static void waitForStackStatus(IntegrationTestContext itContext, String stackId, String desiredStatus, String statusPath) {
        String stackStatus = null;
        int retryCount = 0;
        do {
            LOGGER.info("Waiting for stack status {}, stack id: {}, current status {} ...", desiredStatus, stackId, stackStatus);
            sleep();
            Response response = RestUtil.entityPathRequest(itContext.getContextParam(CloudbreakITContextConstants.CLOUDBREAK_SERVER),
                    itContext.getContextParam(IntegrationTestContext.AUTH_TOKEN), "stackId", stackId).
                    get("stacks/{stackId}/status");
            JsonPath stack = response.jsonPath();
            stack.prettyPrint();
            if (response.getStatusCode() != HttpStatus.OK.value() && response.getStatusCode() != HttpStatus.NOT_FOUND.value()) {
                continue;
            }
            stackStatus = stack.get(statusPath);
            if (stackStatus == null) {
                stackStatus = "DELETE_COMPLETED";
            }
            retryCount++;
        } while (!desiredStatus.equals(stackStatus) && !stackStatus.contains("FAILED") && retryCount < MAX_RETRY);
        LOGGER.info("Stack {} is in desired status {}", stackId, stackStatus);
        if (stackStatus.contains("FAILED")) {
            Assert.fail("The stack has failed");
        }
        if (retryCount == MAX_RETRY) {
            Assert.fail("Timeout happened");
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(POLLING_INTERVAL);
        } catch (InterruptedException e) {
            LOGGER.warn("Ex during wait: {}", e);
        }
    }
}
