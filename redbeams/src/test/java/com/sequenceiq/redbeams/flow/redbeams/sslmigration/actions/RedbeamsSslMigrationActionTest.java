package com.sequenceiq.redbeams.flow.redbeams.sslmigration.actions;

import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus.DB_SSL_MIGRATION_IN_PROGRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationHandlerRequest;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@ExtendWith(MockitoExtension.class)
class RedbeamsSslMigrationActionTest {

    private static final Long RESOURCE_ID = 123L;

    private static final String DB_STACK_NAME = "test-db-stack";

    private static final String REGION = "us-west-2";

    private static final String AVAILABILITY_ZONE = "us-west-2a";

    private static final String RESOURCE_CRN = "crn:cdp:redbeams:us-west-1:accountId:redbeam:dbId";

    private static final String ACCOUNT_ID = "accountId";

    private static final String USER_ID = "userId";

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

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
    private RedbeamsSslMigrationAction underTest;

    @Test
    void testDoExecute() throws Exception {
        RedbeamsSslMigrationEvent payload = new RedbeamsSslMigrationEvent("selector", RESOURCE_ID);
        CloudContext cloudContext = getCloudContext();
        RedbeamsSslMigrationContext context = getRedbeamsSslMigrationContext(cloudContext);

        underTest.doExecute(context, payload, null);

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DB_SSL_MIGRATION_IN_PROGRESS);
    }

    @Test
    void testCreateRequest() {
        CloudContext cloudContext = getCloudContext();
        RedbeamsSslMigrationContext context = getRedbeamsSslMigrationContext(cloudContext);

        Selectable selectable = underTest.createRequest(context);

        assertNotNull(selectable);
        assertInstanceOf(RedbeamsSslMigrationHandlerRequest.class, selectable);

        RedbeamsSslMigrationHandlerRequest request = (RedbeamsSslMigrationHandlerRequest) selectable;
        assertEquals(RESOURCE_ID, request.getResourceId());
        assertEquals(cloudContext, request.getCloudContext());
        assertEquals(cloudCredential, request.getCloudCredential());
        assertEquals(databaseStack, request.getDatabaseStack());
    }

    @Test
    void testGetFailurePayload() {
        RedbeamsSslMigrationEvent payload = new RedbeamsSslMigrationEvent("selector", RESOURCE_ID);
        Exception exception = new RuntimeException("Test exception");

        Object failurePayload = underTest.getFailurePayload(payload, Optional.empty(), exception);

        assertNotNull(failurePayload);
        assertInstanceOf(RedbeamsSslMigrationFailed.class, failurePayload);

        RedbeamsSslMigrationFailed failedEvent = (RedbeamsSslMigrationFailed) failurePayload;
        assertEquals(RESOURCE_ID, failedEvent.getResourceId());
        assertEquals(exception, failedEvent.getException());
    }

    @Test
    void testConstructor() {
        assertNotNull(underTest);
    }

    private RedbeamsSslMigrationContext getRedbeamsSslMigrationContext(CloudContext cloudContext) {
        return new RedbeamsSslMigrationContext(
                flowParameters,
                cloudContext,
                cloudCredential,
                databaseStack,
                dbStack);
    }

    private CloudContext getCloudContext() {
        return CloudContext.Builder.builder()
                .withId(RESOURCE_ID)
                .withName(DB_STACK_NAME)
                .withCrn(RESOURCE_CRN)
                .withPlatform(CloudPlatform.AWS.name())
                .withVariant(CloudPlatform.AWS.name())
                .withLocation(location(region(REGION), availabilityZone(AVAILABILITY_ZONE)))
                .withUserName(USER_ID)
                .withAccountId(ACCOUNT_ID)
                .build();
    }
}
