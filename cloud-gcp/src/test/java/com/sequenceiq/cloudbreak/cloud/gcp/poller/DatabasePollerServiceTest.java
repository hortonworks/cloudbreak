package com.sequenceiq.cloudbreak.cloud.gcp.poller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;

@ExtendWith(MockitoExtension.class)
public class DatabasePollerServiceTest {

    private GcpDatabasePollerProvider databasePollerProvider = mock(GcpDatabasePollerProvider.class);

    private DatabasePollerService underTest = new DatabasePollerService(databasePollerProvider);

    private AttemptMaker attemptMaker = mock(AttemptMaker.class);

    private CloudResource cloudResource = mock(CloudResource.class);

    @BeforeEach
    public void before() {
        ReflectionTestUtils.setField(underTest, "attemptCount", 1);
        ReflectionTestUtils.setField(underTest, "sleepTime", 1);
    }

    @Test
    public void testLaunchDatabasePoller() throws Exception {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("country")))
                .withUserId("user")
                .withAccountId("account")
                .build();
        CloudCredential credential = new CloudCredential();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, credential);

        when(attemptMaker.process()).thenReturn(AttemptResults.justFinish());
        when(databasePollerProvider.launchDatabasePoller(any(AuthenticatedContext.class), anyList())).thenReturn(attemptMaker);

        underTest.launchDatabasePoller(authenticatedContext, List.of(cloudResource));
        verify(attemptMaker, atLeast(1)).process();
    }

    @Test
    public void testStopStartDatabasePoller() throws Exception {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("country")))
                .withUserId("user")
                .withAccountId("account")
                .build();
        CloudCredential credential = new CloudCredential();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, credential);

        when(attemptMaker.process()).thenReturn(AttemptResults.justFinish());
        when(databasePollerProvider.stopStartDatabasePoller(any(AuthenticatedContext.class), anyList())).thenReturn(attemptMaker);

        underTest.startDatabasePoller(authenticatedContext, List.of(cloudResource));
        verify(attemptMaker, atLeast(1)).process();
    }

    @Test
    public void testStopDatabasePoller() throws Exception {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("country")))
                .withUserId("user")
                .withAccountId("account")
                .build();
        CloudCredential credential = new CloudCredential();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, credential);

        when(attemptMaker.process()).thenReturn(AttemptResults.justFinish());
        when(databasePollerProvider.stopStartDatabasePoller(any(AuthenticatedContext.class), anyList())).thenReturn(attemptMaker);

        underTest.stopDatabasePoller(authenticatedContext, List.of(cloudResource));
        verify(attemptMaker, atLeast(1)).process();
    }

    @Test
    public void testInsertUserPoller() throws Exception {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("country")))
                .withUserId("user")
                .withAccountId("account")
                .build();
        CloudCredential credential = new CloudCredential();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, credential);

        when(attemptMaker.process()).thenReturn(AttemptResults.justFinish());
        when(databasePollerProvider.insertUserPoller(any(AuthenticatedContext.class), anyList())).thenReturn(attemptMaker);

        underTest.insertUserPoller(authenticatedContext, List.of(cloudResource));
        verify(attemptMaker, atLeast(1)).process();
    }

    @Test
    public void testTerminateDatabasePoller() throws Exception {
        CloudContext context = CloudContext.Builder.builder()
                .withId(1L)
                .withName("context")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("country")))
                .withUserId("user")
                .withAccountId("account")
                .build();
        CloudCredential credential = new CloudCredential();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(context, credential);

        when(attemptMaker.process()).thenReturn(AttemptResults.justFinish());
        when(databasePollerProvider.terminateDatabasePoller(any(AuthenticatedContext.class), anyList())).thenReturn(attemptMaker);

        underTest.terminateDatabasePoller(authenticatedContext, List.of(cloudResource));
        verify(attemptMaker, atLeast(1)).process();
    }

}