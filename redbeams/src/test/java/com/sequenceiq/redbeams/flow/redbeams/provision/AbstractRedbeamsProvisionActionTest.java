package com.sequenceiq.redbeams.flow.redbeams.provision;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.statemachine.StateContext;

public class AbstractRedbeamsProvisionActionTest {

    @Mock
    private DBStackService dbStackService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @InjectMocks
    private AbstractRedbeamsProvisionAction underTest = new TestAction(TestPayload.class);

    private DBStack dbStack;

    private Credential credential;

    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<RedbeamsProvisionState, RedbeamsProvisionEvent> stateContext;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        dbStack = new DBStack();
        dbStack.setId(101L);
        dbStack.setName("mystack");
        dbStack.setRegion("us-east-1");
        dbStack.setAvailabilityZone("us-east-1b");
        dbStack.setCloudPlatform("AWS");
        dbStack.setPlatformVariant("GovCloud");
        dbStack.setEnvironmentId("myenv");
        dbStack.setOwnerCrn(Crn.safeFromString("crn:altus:iam:us-west-1:cloudera:user:bob@cloudera.com"));

        credential = new Credential("userId", null, "userCrn");

        cloudCredential = new CloudCredential("userId", "userName");
    }

    @Test
    public void testCreateFlowContext() {
        when(dbStackService.getById(Long.valueOf(1L))).thenReturn(dbStack);
        when(credentialService.getCredentialByEnvCrn("myenv")).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(databaseStackConverter.convert(dbStack)).thenReturn(databaseStack);

        RedbeamsContext ctx = underTest.createFlowContext(flowParameters, stateContext, new TestPayload());

        assertEquals(dbStack.getId(), ctx.getCloudContext().getId());
        assertEquals(dbStack.getName(), ctx.getCloudContext().getName());
        assertEquals(Platform.platform(dbStack.getCloudPlatform()), ctx.getCloudContext().getPlatform());
        assertEquals(Variant.variant(dbStack.getPlatformVariant()), ctx.getCloudContext().getVariant());
        assertEquals(Location.location(Region.region(dbStack.getRegion()), AvailabilityZone.availabilityZone(dbStack.getAvailabilityZone())),
            ctx.getCloudContext().getLocation());
        assertEquals(dbStack.getOwnerCrn().getResource(), ctx.getCloudContext().getUserId());
        assertEquals(dbStack.getOwnerCrn().getAccountId(), ctx.getCloudContext().getAccountId());

        assertEquals(cloudCredential, ctx.getCloudCredential());

        assertEquals(databaseStack, ctx.getDatabaseStack());
    }

    private static class TestPayload implements Payload {
        @Override
        public Long getResourceId() {
            return 1L;
        }
    }

    private static class TestAction extends AbstractRedbeamsProvisionAction<TestPayload> {
        TestAction(Class<TestPayload> payloadClass) {
            super(payloadClass);
        }
    }
}
