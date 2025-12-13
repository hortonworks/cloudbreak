package com.sequenceiq.redbeams.flow.redbeams.termination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
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
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

class AbstractRedbeamsTerminationActionTest {

    @Mock
    private DBStackService dbStackService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private DBStackToDatabaseStackConverter databaseStackConverter;

    @InjectMocks
    private AbstractRedbeamsTerminationAction underTest = new TestAction(RedbeamsEvent.class);

    private DBStack dbStack;

    private Credential credential;

    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private StateContext<RedbeamsTerminationState, RedbeamsTerminationEvent> stateContext;

    @BeforeEach
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
        dbStack.setOwnerCrn(Crn.safeFromString("crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com"));
        dbStack.setResourceCrn(CrnTestUtil.getDatabaseCrnBuilder()
                .setAccountId("acc")
                .setResource("resource")
                .build().toString());

        credential = new Credential("userId", null, "userCrn", "account");

        cloudCredential = new CloudCredential("userId", "userName", "account");
    }

    @Test
    void testCreateFlowContext() {
        when(dbStackService.findById(Long.valueOf(1L))).thenReturn(Optional.of(dbStack));
        when(credentialService.getCredentialByEnvCrn("myenv")).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(databaseStackConverter.convert(dbStack)).thenReturn(databaseStack);

        RedbeamsContext ctx = underTest.createFlowContext(flowParameters, stateContext, new RedbeamsEvent(1L));

        assertEquals(dbStack.getId(), ctx.getCloudContext().getId());
        assertEquals(dbStack.getName(), ctx.getCloudContext().getName());
        assertEquals(Platform.platform(dbStack.getCloudPlatform()), ctx.getCloudContext().getPlatform());
        assertEquals(Variant.variant(dbStack.getPlatformVariant()), ctx.getCloudContext().getVariant());
        assertEquals(Location.location(Region.region(dbStack.getRegion()), AvailabilityZone.availabilityZone(dbStack.getAvailabilityZone())),
                ctx.getCloudContext().getLocation());
        assertEquals(dbStack.getOwnerCrn().getAccountId(), ctx.getCloudContext().getAccountId());

        assertEquals(cloudCredential, ctx.getCloudCredential());

        assertEquals(databaseStack, ctx.getDatabaseStack());
    }

    private static class TestAction extends AbstractRedbeamsTerminationAction<RedbeamsEvent> {
        TestAction(Class<RedbeamsEvent> payloadClass) {
            super(payloadClass);
        }
    }
}
