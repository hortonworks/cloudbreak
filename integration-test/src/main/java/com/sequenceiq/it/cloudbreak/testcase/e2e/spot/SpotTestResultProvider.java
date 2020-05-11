package com.sequenceiq.it.cloudbreak.testcase.e2e.spot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;

class SpotTestResultProvider {

    static final String AWS_INSUFFICIENT_SPOT_CAPACITY_MESSAGE = "Could not launch Spot Instances. " +
            "InsufficientInstanceCapacity - There is no Spot capacity available that matches your request. Launching EC2 instance failed.";

    private static final Logger LOGGER = LoggerFactory.getLogger(SpotTestResultProvider.class);

    private static final String MESSAGE_PREFIX = "[Spot test result]";

    private final String type;

    SpotTestResultProvider(String type) {
        this.type = type;
    }

    void insufficientCapacity() {
        String message = String.format("%s %s tried to start on spot instances, but aws capacity was insufficient.", MESSAGE_PREFIX, type);
        LOGGER.info(message);
    }

    void runsOnSpotInstances() {
        String message = String.format("%s %s is running on spot instances.", MESSAGE_PREFIX, type);
        LOGGER.info(message);
    }

    void fail(Object response) {
        String message = String.format("%s %s was not started on spot instances. Response: %s", MESSAGE_PREFIX, type, response);
        Assert.fail(message);
    }
}
