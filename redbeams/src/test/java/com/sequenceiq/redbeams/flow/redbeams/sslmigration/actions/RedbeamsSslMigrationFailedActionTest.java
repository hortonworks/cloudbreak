package com.sequenceiq.redbeams.flow.redbeams.sslmigration.actions;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus.DB_SSL_MIGRATION_FAILED;
import static com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors.REDBEAMS_SSL_MIGRATION_FAILURE_HANDLED_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.FlowRegister;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationContext;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationEvent;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationFailed;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@ExtendWith(MockitoExtension.class)
class RedbeamsSslMigrationFailedActionTest {

    private static final Long RESOURCE_ID = 123L;

    private static final String DB_STACK_NAME = "test-db-stack";

    private static final String REGION = "us-west-2";

    private static final String AVAILABILITY_ZONE = "us-west-2a";

    private static final String RESOURCE_CRN = "crn:cdp:redbeams:us-west-1:accountId:redbeam:dbId";

    private static final String ACCOUNT_ID = "accountId";

    private static final String USER_ID = "userId";

    private static final String FLOW_ID = "flowId";

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private RedbeamsMetricService metricService;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private DBStack dbStack;

    @Mock
    private FlowRegister runningFlows;

    @Mock
    private EventBus eventBus;

    @Mock
    private ErrorHandlerAwareReactorEventFactory reactorEventFactory;

    @InjectMocks
    private RedbeamsSslMigrationFailedAction underTest;

    @Test
    void testDoExecuteWithException() throws Exception {
        Exception exception = new RuntimeException("Test exception");
        RedbeamsSslMigrationFailed payload = new RedbeamsSslMigrationFailed(RESOURCE_ID, exception);
        Optional<DBStack> dbStackOptional = Optional.of(dbStack);

        RedbeamsSslMigrationContext context = getRedbeamsSslMigrationContext();

        when(dbStackStatusUpdater.updateStatus(RESOURCE_ID, DB_SSL_MIGRATION_FAILED, "Test exception"))
                .thenReturn(dbStackOptional);

        underTest.doExecute(context, payload, null);

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DB_SSL_MIGRATION_FAILED, "Test exception");
        verify(metricService).incrementMetricCounter(MetricType.DB_SSL_MIGRATION_FAILED, dbStackOptional);
    }

    @Test
    void testDoExecuteWithoutException() throws Exception {
        RedbeamsSslMigrationFailed payload = new RedbeamsSslMigrationFailed(RESOURCE_ID, null);
        Optional<DBStack> dbStackOptional = Optional.of(dbStack);

        RedbeamsSslMigrationContext context = getRedbeamsSslMigrationContext();

        when(dbStackStatusUpdater.updateStatus(RESOURCE_ID, DB_SSL_MIGRATION_FAILED, "Unknown error"))
                .thenReturn(dbStackOptional);

        underTest.doExecute(context, payload, null);

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DB_SSL_MIGRATION_FAILED, "Unknown error");
        verify(metricService).incrementMetricCounter(MetricType.DB_SSL_MIGRATION_FAILED, dbStackOptional);
    }

    @Test
    void testCreateRequest() {
        when(dbStack.getId()).thenReturn(RESOURCE_ID);

        RedbeamsSslMigrationContext context = getRedbeamsSslMigrationContext();

        Selectable selectable = underTest.createRequest(context);

        assertNotNull(selectable);
        assertInstanceOf(RedbeamsSslMigrationEvent.class, selectable);

        RedbeamsSslMigrationEvent event = (RedbeamsSslMigrationEvent) selectable;
        assertEquals(REDBEAMS_SSL_MIGRATION_FAILURE_HANDLED_EVENT.event(), event.selector());
        assertEquals(RESOURCE_ID, event.getResourceId());
    }

    @Test
    void testGetFailurePayload() {
        Exception exception = new RuntimeException("Test exception");
        RedbeamsSslMigrationFailed payload = new RedbeamsSslMigrationFailed(RESOURCE_ID, exception);
        Exception newException = new RuntimeException("New exception");

        Object failurePayload = underTest.getFailurePayload(payload, Optional.empty(), newException);

        assertNotNull(failurePayload);
        assertInstanceOf(RedbeamsSslMigrationFailed.class, failurePayload);

        RedbeamsSslMigrationFailed failedEvent = (RedbeamsSslMigrationFailed) failurePayload;
        assertEquals(RESOURCE_ID, failedEvent.getResourceId());
        assertEquals(newException, failedEvent.getException());
    }

    @Test
    void testConstructor() {
        assertNotNull(underTest);
    }

    private RedbeamsSslMigrationContext getRedbeamsSslMigrationContext() {
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(RESOURCE_ID)
                .withName(DB_STACK_NAME)
                .withCrn(RESOURCE_CRN)
                .withPlatform(CloudPlatform.AWS.name())
                .withVariant(CloudPlatform.AWS.name())
                .withLocation(location(region(REGION), availabilityZone(AVAILABILITY_ZONE)))
                .withUserName(USER_ID)
                .withAccountId(ACCOUNT_ID)
                .build();

        return new RedbeamsSslMigrationContext(
                flowParameters,
                cloudContext,
                cloudCredential,
                databaseStack,
                dbStack);
    }
}
