package com.sequenceiq.redbeams.flow.redbeams.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.redbeams.converter.spi.DBStackToDatabaseStackConverter;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.dto.Credential;
import com.sequenceiq.redbeams.service.CredentialService;
import com.sequenceiq.redbeams.service.stack.DBStackService;

@ExtendWith(MockitoExtension.class)
class RedbeamsFlowContextBuilderTest {

    private static final Long RESOURCE_ID = 1L;

    private static final String ACCOUNT_ID = "accountId";

    private static final String OWNER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:username";

    private static final String RESOURCE_CRN = "crn:cdp:redbeams:us-west-1:" + ACCOUNT_ID + ":databaseServer:name";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:env";

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
    private Credential credential;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private DatabaseStack databaseStack;

    @InjectMocks
    private RedbeamsFlowContextBuilder underTest;

    @Test
    void testCreateRedbeamsContext() {
        DBStack dbStack = createDBStack();
        when(dbStackService.getById(RESOURCE_ID)).thenReturn(dbStack);
        when(credentialService.getCredentialByEnvCrn(ENVIRONMENT_CRN)).thenReturn(credential);
        when(credentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(databaseStackConverter.convert(dbStack)).thenReturn(databaseStack);

        RedbeamsContext result = underTest.createRedbeamsContext(flowParameters, RESOURCE_ID);

        assertSame(flowParameters, result.getFlowParameters());
        assertSame(cloudCredential, result.getCloudCredential());
        assertSame(databaseStack, result.getDatabaseStack());
        assertSame(dbStack, result.getDBStack());
        CloudContext cloudContext = result.getCloudContext();
        assertEquals(RESOURCE_ID, cloudContext.getId());
        assertEquals("name", cloudContext.getName());
        assertEquals(RESOURCE_CRN, cloudContext.getCrn());
        assertEquals("AWS", cloudContext.getPlatform().value());
        assertEquals("AWS_VARIANT", cloudContext.getVariant().value());
        assertEquals("us-west-2", cloudContext.getLocation().getRegion().getRegionName());
        assertEquals("us-west-2a", cloudContext.getLocation().getAvailabilityZone().value());
        assertEquals("username", cloudContext.getUserName());
        assertEquals(ACCOUNT_ID, cloudContext.getAccountId());
    }

    private DBStack createDBStack() {
        DBStack dbStack = new DBStack();
        dbStack.setId(RESOURCE_ID);
        dbStack.setName("name");
        dbStack.setResourceCrn(RESOURCE_CRN);
        dbStack.setCloudPlatform("AWS");
        dbStack.setPlatformVariant("AWS_VARIANT");
        dbStack.setRegion("us-west-2");
        dbStack.setAvailabilityZone("us-west-2a");
        dbStack.setUserName("username");
        dbStack.setOwnerCrn(Crn.fromString(OWNER_CRN));
        dbStack.setEnvironmentId(ENVIRONMENT_CRN);
        return dbStack;
    }
}
