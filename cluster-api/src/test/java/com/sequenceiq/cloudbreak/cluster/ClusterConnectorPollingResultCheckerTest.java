package com.sequenceiq.cloudbreak.cluster;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.service.ClusterException;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.cluster.service.ClusterConnectorPollingResultChecker;

@RunWith(MockitoJUnitRunner.class)
public class ClusterConnectorPollingResultCheckerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private final ClusterConnectorPollingResultChecker underTest = new ClusterConnectorPollingResultChecker();

    @Test
    public void testCheckPollingResultWhenExited() throws ClusterException {
        thrown.expect(CancellationException.class);
        thrown.expectMessage("Stack or cluster in delete in progress phase.");

        underTest.checkPollingResult(PollingResult.EXIT, "exited");
    }

    @Test
    public void testCheckPollingResultWhenFailedThenShouldReturnFailedMessage() throws ClusterException {
        thrown.expect(ClusterException.class);
        thrown.expectMessage("failed");

        underTest.checkPollingResult(PollingResult.FAILURE, "failed");
    }

    @Test
    public void testCheckPollingResultWhenTimeOutThenShouldReturnTimeOutMessage() throws ClusterException {
        thrown.expect(ClusterException.class);
        thrown.expectMessage("timeout");

        underTest.checkPollingResult(PollingResult.TIMEOUT, "timeout");
    }
}