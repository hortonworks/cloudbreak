package com.sequenceiq.redbeams.flow.redbeams.provision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.auth.CrnTestUtil;
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

@ExtendWith(MockitoExtension.class)
public class AbstractRedbeamsProvisionActionTest {

    private static final long RESOURCE_ID = 1L;

    private static final String ENVIRONMENT_CRN = "myenv";

    @Mock
    private DBStackService dbStackService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @InjectMocks
    private TestAction underTest;

    private DBStack dbStack;

    private Credential credential;

    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<RedbeamsProvisionState, RedbeamsProvisionEvent> stateContext;

    @BeforeEach
    public void setUp() throws Exception {
        dbStack = new DBStack();
        dbStack.setId(101L);
        dbStack.setResourceCrn(CrnTestUtil.getDatabaseServerCrnBuilder()
                .setAccountId("acc")
                .setResource("resource")
                .build());
        dbStack.setName("mystack");
        dbStack.setRegion("us-east-1");
        dbStack.setAvailabilityZone("us-east-1b");
        dbStack.setCloudPlatform("AWS");
        dbStack.setPlatformVariant("GovCloud");
        dbStack.setEnvironmentId(ENVIRONMENT_CRN);
        dbStack.setOwnerCrn(Crn.safeFromString("crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com"));

        credential = new Credential("userId", null, "userCrn");

        cloudCredential = new CloudCredential("userId", "userName");
    }

    @Test
    public void testCreateFlowContext() {
        when(dbStackService.getById(RESOURCE_ID)).thenReturn(dbStack);
        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_CRN)).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(databaseStackConverter.convert(dbStack)).thenReturn(databaseStack);

        RedbeamsContext ctx = underTest.createFlowContext(flowParameters, stateContext, new TestPayload());

        assertEquals(dbStack.getId(), ctx.getCloudContext().getId());
        assertEquals(dbStack.getName(), ctx.getCloudContext().getName());
        assertEquals(Platform.platform(dbStack.getCloudPlatform()), ctx.getCloudContext().getPlatform());
        assertEquals(Variant.variant(dbStack.getPlatformVariant()), ctx.getCloudContext().getVariant());
        assertEquals(Location.location(Region.region(dbStack.getRegion()), AvailabilityZone.availabilityZone(dbStack.getAvailabilityZone())),
            ctx.getCloudContext().getLocation());
        assertEquals(dbStack.getOwnerCrn().getUserId(), ctx.getCloudContext().getUserId());
        assertEquals(dbStack.getOwnerCrn().getAccountId(), ctx.getCloudContext().getAccountId());

        assertEquals(cloudCredential, ctx.getCloudCredential());

        assertEquals(databaseStack, ctx.getDatabaseStack());
    }

    private static class TestPayload implements Payload {
        @Override
        public Long getResourceId() {
            return RESOURCE_ID;
        }
    }

    private static class TestAction extends AbstractRedbeamsProvisionAction<TestPayload> {
        TestAction() {
            super(TestPayload.class);
        }
    }

}
