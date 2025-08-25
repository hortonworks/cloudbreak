package com.sequenceiq.redbeams.flow.redbeams.sslmigration.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateContext;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationContext;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationEventSelectors;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.RedbeamsSslMigrationState;
import com.sequenceiq.redbeams.flow.redbeams.sslmigration.event.RedbeamsSslMigrationEvent;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class AbstractRedbeamsSslMigrationActionTest {

    private static final Long RESOURCE_ID = 123L;

    private static final String DB_STACK_NAME = "test-db-stack";

    private static final String REGION = "us-west-2";

    private static final String AVAILABILITY_ZONE = "us-west-2a";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:envId";

    private static final String RESOURCE_CRN = "crn:cdp:redbeams:us-west-1:accountId:redbeam:dbId";

    private static final String OWNER_CRN = "crn:cdp:iam:us-west-1:accountId:user:userId";

    @Mock
    private DBStackService dbStackService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<RedbeamsSslMigrationState, RedbeamsSslMigrationEventSelectors> stateContext;

    @Mock
    private Credential credential;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @InjectMocks
    private TestRedbeamsSslMigrationAction underTest;

    private DBStack dbStack;

    @BeforeEach
    void setUp() {
        dbStack = new DBStack();
        dbStack.setId(RESOURCE_ID);
        dbStack.setResourceCrn(RESOURCE_CRN);
        dbStack.setName(DB_STACK_NAME);
        dbStack.setRegion(REGION);
        dbStack.setAvailabilityZone(AVAILABILITY_ZONE);
        dbStack.setCloudPlatform("AWS");
        dbStack.setPlatformVariant("AWS");
        dbStack.setEnvironmentId(ENVIRONMENT_CRN);
        dbStack.setOwnerCrn(Crn.safeFromString(OWNER_CRN));

        ReflectionTestUtils.setField(underTest, "dbStackService", dbStackService);
        ReflectionTestUtils.setField(underTest, "credentialService", credentialService);
        ReflectionTestUtils.setField(underTest, "credentialConverter", credentialConverter);
        ReflectionTestUtils.setField(underTest, "databaseStackConverter", databaseStackConverter);
    }

    @Test
    void testCreateFlowContext() {
        RedbeamsSslMigrationEvent payload = new RedbeamsSslMigrationEvent("selector", RESOURCE_ID);

        when(dbStackService.getById(RESOURCE_ID)).thenReturn(dbStack);
        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_CRN)).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(databaseStackConverter.convert(dbStack)).thenReturn(databaseStack);

        RedbeamsSslMigrationContext context = underTest.createFlowContext(flowParameters, stateContext, payload);

        assertNotNull(context);
        assertEquals(dbStack, context.getDBStack());
        assertEquals(cloudCredential, context.getCloudCredential());
        assertEquals(databaseStack, context.getDatabaseStack());

        assertEquals(dbStack.getId(), context.getCloudContext().getId());
        assertEquals(dbStack.getName(), context.getCloudContext().getName());
        assertEquals(Platform.platform(dbStack.getCloudPlatform()), context.getCloudContext().getPlatform());
        assertEquals(Variant.variant(dbStack.getPlatformVariant()), context.getCloudContext().getVariant());
        assertEquals(Location.location(Region.region(dbStack.getRegion()), AvailabilityZone.availabilityZone(dbStack.getAvailabilityZone())),
                context.getCloudContext().getLocation());
        assertEquals(dbStack.getOwnerCrn().getAccountId(), context.getCloudContext().getAccountId());
    }

    private static class TestRedbeamsSslMigrationAction extends AbstractRedbeamsSslMigrationAction<RedbeamsSslMigrationEvent> {

        protected TestRedbeamsSslMigrationAction() {
            super(RedbeamsSslMigrationEvent.class);
        }

        @Override
        protected void doExecute(RedbeamsSslMigrationContext context, RedbeamsSslMigrationEvent payload, Map<Object, Object> variables) throws Exception {

        }

        @Override
        protected Object getFailurePayload(RedbeamsSslMigrationEvent payload,
                java.util.Optional<RedbeamsSslMigrationContext> flowContext, Exception ex) {
            return null;
        }
    }
}
