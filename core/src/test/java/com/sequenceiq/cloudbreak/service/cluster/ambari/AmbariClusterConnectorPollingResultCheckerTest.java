package com.sequenceiq.cloudbreak.service.cluster.ambari;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.core.ClusterException;
import com.sequenceiq.cloudbreak.service.PollingResult;

@RunWith(MockitoJUnitRunner.class)
public class AmbariClusterConnectorPollingResultCheckerTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private AmbariClusterConnectorPollingResultChecker underTest = new AmbariClusterConnectorPollingResultChecker();

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