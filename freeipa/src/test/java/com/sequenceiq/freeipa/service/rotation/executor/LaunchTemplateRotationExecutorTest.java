package com.sequenceiq.freeipa.service.rotation.executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.freeipa.converter.cloud.CredentialToCloudCredentialConverter;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.converter.cloud.StackToCloudStackConverter;
import com.sequenceiq.freeipa.dto.Credential;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.CredentialService;
import com.sequenceiq.freeipa.service.resource.ResourceService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class LaunchTemplateRotationExecutorTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:accountId:environment:ac5ba74b-c35e-45e9-9f47-123456789876";

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:accountId:environment:ac5ba74b-c35e-45e9-9f47-123456789877";

    @Mock
    private StackService stackService;

    @Mock
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToCloudCredentialConverter credentialConverter;

    @Mock
    private CredentialService credentialService;

    @InjectMocks
    private LaunchTemplateRotationExecutor underTest;

    @Test
    void testRotate() throws Exception {
        Stack stack = new Stack();
        stack.setResourceCrn(FREEIPA_CRN);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        ImageEntity imageEntity = new ImageEntity();
        imageEntity.setGatewayUserdata("userdata");
        stack.setImage(imageEntity);
        when(stackService.getByEnvironmentCrnAndAccountIdWithLists(eq(ENVIRONMENT_CRN), anyString())).thenReturn(stack);
        CloudStack cloudStack = new CloudStack(new HashSet<>(), null, null, new HashMap<>(), new HashMap<>(), "AWS::EC2::LaunchTemplate",
                null, null, null, null, null, null);
        when(stackToCloudStackConverter.convert(eq(stack))).thenReturn(cloudStack);
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        when(credentialService.getCredentialByEnvCrn(eq(ENVIRONMENT_CRN))).thenReturn(new Credential(null, null, null, null, null));
        when(credentialConverter.convert(any())).thenReturn(new CloudCredential());
        Authenticator authenticator = mock(Authenticator.class);
        when(cloudConnector.authentication()).thenReturn(authenticator);
        ResourceConnector resourceConnector = mock(ResourceConnector.class);
        when(cloudConnector.resources()).thenReturn(resourceConnector);
        underTest.rotate(new RotationContext(ENVIRONMENT_CRN));
        verify(resourceConnector, times(1)).updateUserData(any(), eq(cloudStack), anyList(), anyMap());
    }

}