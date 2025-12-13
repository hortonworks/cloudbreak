package com.sequenceiq.cloudbreak.cluster;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterConnectorPollingResultChecker;
import com.sequenceiq.cloudbreak.cluster.service.ClusterException;
import com.sequenceiq.cloudbreak.polling.PollingResult;

class ClusterConnectorPollingResultCheckerTest {

    private final ClusterConnectorPollingResultChecker underTest = new ClusterConnectorPollingResultChecker();

    @Test
    void testCheckPollingResultWhenExited() throws ClusterException {
        assertThrows(CancellationException.class, () -> underTest.checkPollingResult(PollingResult.EXIT, "exited"),
                "Stack or cluster in delete in progress phase.");
    }

    @Test
    void testCheckPollingResultWhenFailedThenShouldReturnFailedMessage() throws ClusterException {
        assertThrows(ClusterException.class, () -> underTest.checkPollingResult(PollingResult.FAILURE, "failed"), "failed");
    }

    @Test
    void testCheckPollingResultWhenTimeOutThenShouldReturnTimeOutMessage() throws ClusterException {
        assertThrows(ClusterException.class, () -> underTest.checkPollingResult(PollingResult.TIMEOUT, "timeout"), "timeout");
    }
}