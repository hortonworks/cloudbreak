package com.sequenceiq.cloudbreak.cloud.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesRequest;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.model.CredentialType;

@ExtendWith(MockitoExtension.class)
public class CredentialPrerequisitesHandlerTest {

    private static final String EXTERNAL_ID = "externalId";

    private static final String AUDIT_EXTERNAL_ID = "auditExternalId";

    private static final String DEPLOYMENT_ADDRESS = "deploymentAddress";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private CredentialConnector credentialConnector;

    @InjectMocks
    private CredentialPrerequisitesHandler underTest;

    @Test
    void testAcceptShouldPropagateGovCloudFlagFromRequestWhenGovCloud() {
        assertGovCloudFlagPropagated(true);
    }

    @Test
    void testAcceptShouldPropagateGovCloudFlagFromRequestWhenNotGovCloud() {
        assertGovCloudFlagPropagated(false);
    }

    private void assertGovCloudFlagPropagated(boolean govCloud) {
        CloudContext cloudContext = CloudContext.Builder.builder().withPlatform("AWS").build();
        CredentialPrerequisitesRequest request = new CredentialPrerequisitesRequest(cloudContext, EXTERNAL_ID, AUDIT_EXTERNAL_ID, DEPLOYMENT_ADDRESS,
                CredentialType.ENVIRONMENT, govCloud);
        when(cloudPlatformConnectors.getDefault(cloudContext.getPlatform())).thenReturn(cloudConnector);
        when(cloudConnector.credentials()).thenReturn(credentialConnector);
        when(credentialConnector.getPrerequisites(cloudContext, EXTERNAL_ID, AUDIT_EXTERNAL_ID, DEPLOYMENT_ADDRESS, CredentialType.ENVIRONMENT, govCloud))
                .thenReturn(mock(CredentialPrerequisitesResponse.class));

        underTest.accept(new Event<>(request));

        verify(credentialConnector).getPrerequisites(cloudContext, EXTERNAL_ID, AUDIT_EXTERNAL_ID, DEPLOYMENT_ADDRESS, CredentialType.ENVIRONMENT, govCloud);
    }
}
