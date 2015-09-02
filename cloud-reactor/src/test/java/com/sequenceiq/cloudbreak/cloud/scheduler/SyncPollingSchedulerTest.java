package com.sequenceiq.cloudbreak.cloud.scheduler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.sequenceiq.cloudbreak.cloud.BooleanStateConnector;
import com.sequenceiq.cloudbreak.cloud.ReactorTestUtil;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.instance.BooleanResult;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@RunWith(MockitoJUnitRunner.class)
public class SyncPollingSchedulerTest {
    @Mock
    private BooleanStateConnector booleanStateExceptionConnector;
    private SyncPollingScheduler underTest;

    @Before
    public void before() {
        underTest = new SyncPollingScheduler();
        ReflectionTestUtils.setField(underTest, "scheduler", MoreExecutors.listeningDecorator(
                new ScheduledThreadPoolExecutor(10, new ThreadFactoryBuilder().setNameFormat("cloud-reactor-%d").build())));
    }

    @Test
    public void verifyThreeCallWhenUsingDefaultExceptionCount() throws InterruptedException, ExecutionException, TimeoutException {
        Exception exception = null;
        when(booleanStateExceptionConnector.check(any(AuthenticatedContext.class))).thenThrow(new CloudConnectorException("test"));
        try {
            underTest.schedule(new PollTestBooleanStateTask(booleanStateExceptionConnector));
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        verify(booleanStateExceptionConnector, times(3)).check(any(AuthenticatedContext.class));
    }

    @Test
    public void verifyFirstCallOkWhenUsingDefaultExceptionCount() throws InterruptedException, ExecutionException, TimeoutException {
        Exception exception = null;
        when(booleanStateExceptionConnector.check(any(AuthenticatedContext.class))).thenReturn(Boolean.TRUE);
        try {
            underTest.schedule(new PollTestBooleanStateTask(booleanStateExceptionConnector));
        } catch (Exception e) {
            exception = e;
        }
        assertNull(exception);
        verify(booleanStateExceptionConnector, times(1)).check(any(AuthenticatedContext.class));
    }

    @Test
    public void verifyThreeCallWhenThirdIsOkUsingDefaultExceptionCount() throws InterruptedException, ExecutionException, TimeoutException {
        Exception exception = null;
        try {
            underTest.schedule(new PollTestBooleanStateTask(new BooleanStateSpecificConnector()));
        } catch (Exception e) {
            exception = e;
        }
        assertNull(exception);
    }

    private class BooleanStateSpecificConnector implements BooleanStateConnector {

        private int testCount;

        @Override
        public Boolean check(AuthenticatedContext authenticatedContext) {
            testCount++;
            if (testCount == 3) {
                return Boolean.TRUE;
            } else {
                throw new CloudConnectorException("test");
            }
        }
    }

    private class PollTestBooleanStateTask extends PollTask<BooleanResult> {

        private BooleanStateConnector booleanStateConnector;

        public PollTestBooleanStateTask(BooleanStateConnector booleanStateConnector) {
            super(new AuthenticatedContext(new CloudContext(ReactorTestUtil.stack()), new CloudCredential("a", "b")));
            this.booleanStateConnector = booleanStateConnector;
        }

        @Override
        public BooleanResult call() throws Exception {
            Boolean result = booleanStateConnector.check(getAuthenticatedContext());
            return new BooleanResult(getAuthenticatedContext().getCloudContext(), result);
        }

        @Override
        public boolean completed(BooleanResult booleanResult) {
            return booleanResult.getResult().equals(Boolean.TRUE);
        }
    }

}