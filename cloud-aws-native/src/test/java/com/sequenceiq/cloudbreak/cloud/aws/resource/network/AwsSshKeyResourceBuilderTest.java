package com.sequenceiq.cloudbreak.cloud.aws.resource.network;

import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.authentication.AwsSshKeyResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsPublicKeyConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util.SecurityGroupBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyRegisterRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyUnregisterRequest;
import com.sequenceiq.cloudbreak.cloud.template.init.SshKeyNameGenerator;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AwsSshKeyResourceBuilderTest {

    @InjectMocks
    private AwsSshKeyResourceBuilder awsSshKeyResourceBuilder;

    @Mock
    private SecurityGroupBuilderUtil securityGroupBuilderUtil;

    @Mock
    private AwsMethodExecutor awsMethodExecutor;

    @Mock
    private SshKeyNameGenerator sshKeyNameGenerator;

    @Mock
    private AwsPublicKeyConnector awsPublicKeyConnector;

    @Test
    void testCreateWhenMustUploadPublicKeyReturnsCloudResource() {
        AwsContext context = mock(AwsContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        CloudStack stack = mock(CloudStack.class);
        Location location = mock(Location.class);
        AvailabilityZone availabilityZone = mock(AvailabilityZone.class);

        when(context.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(availabilityZone.value()).thenReturn("us-east-1a");
        when(sshKeyNameGenerator.mustUploadPublicKey(stack)).thenReturn(true);
        when(sshKeyNameGenerator.getKeyPairName(auth, stack)).thenReturn("test-key-pair");

        CloudResource result = awsSshKeyResourceBuilder.create(context, auth, stack);

        assertNotNull(result);
        assertEquals(ResourceType.AWS_SSH_KEY, result.getType());
        assertEquals("test-key-pair", result.getName());
        assertEquals("us-east-1a", result.getAvailabilityZone());

        verify(sshKeyNameGenerator, times(1)).mustUploadPublicKey(stack);
        verify(sshKeyNameGenerator, times(1)).getKeyPairName(auth, stack);
    }

    @Test
    void testCreateWhenMustNotUploadPublicKeyReturnsNull() {
        AwsContext context = mock(AwsContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        CloudStack stack = mock(CloudStack.class);
        Location location = mock(Location.class);
        AvailabilityZone availabilityZone = mock(AvailabilityZone.class);

        when(context.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(availabilityZone.value()).thenReturn("us-east-1a");

        when(sshKeyNameGenerator.mustUploadPublicKey(stack)).thenReturn(false);

        CloudResource result = awsSshKeyResourceBuilder.create(context, auth, stack);

        assertNull(result);

        verify(sshKeyNameGenerator, times(1)).mustUploadPublicKey(stack);
        verifyNoMoreInteractions(sshKeyNameGenerator);
    }

    @Test
    void testDeleteWhenCalledInvokesUnregister() throws Exception {
        AwsContext context = mock(AwsContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        CloudResource resource = mock(CloudResource.class);
        CloudStack stack = mock(CloudStack.class);
        CloudContext cloudContext = mock(CloudContext.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);

        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getLocation()).thenReturn(location(region("us-west-1")));
        when(cloudContext.getPlatform()).thenReturn(platform("AWS"));
        when(resource.getReference()).thenReturn("test-key-pair");
        when(auth.getCloudCredential()).thenReturn(cloudCredential);

        doNothing().when(awsPublicKeyConnector).unregister(any(PublicKeyUnregisterRequest.class));

        CloudResource result = awsSshKeyResourceBuilder.delete(context, auth, resource, stack);

        assertNotNull(result);
        assertEquals(resource, result);

        verify(awsPublicKeyConnector, times(1)).unregister(any(PublicKeyUnregisterRequest.class));
        verify(resource, times(1)).getReference();
        verify(auth.getCloudContext(), times(1)).getPlatform();
    }

    @Test
    void testBuildWhenCalledRegistersPublicKey() throws Exception {
        AwsContext context = mock(AwsContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        CloudStack stack = mock(CloudStack.class);
        CloudResource resource = mock(CloudResource.class);
        CloudCredential cloudCredential = mock(CloudCredential.class);
        InstanceAuthentication instanceAuthentication = mock(InstanceAuthentication.class);
        CloudContext cloudContext = mock(CloudContext.class);

        when(resource.getType()).thenReturn(ResourceType.AWS_SSH_KEY);
        when(resource.getStatus()).thenReturn(CommonStatus.CREATED);
        when(resource.getName()).thenReturn("test-key-pair");

        when(cloudContext.getPlatform()).thenReturn(platform("AWS"));
        when(cloudContext.getLocation()).thenReturn(location(region("us-west-1")));
        when(auth.getCloudContext()).thenReturn(cloudContext);
        when(instanceAuthentication.getPublicKey()).thenReturn("key");
        when(auth.getCloudCredential()).thenReturn(cloudCredential);
        when(sshKeyNameGenerator.getKeyPairName(auth, stack)).thenReturn("test-key-pair");
        when(stack.getInstanceAuthentication()).thenReturn(instanceAuthentication);

        doNothing().when(awsPublicKeyConnector).register(any(PublicKeyRegisterRequest.class));

        CloudResource result = awsSshKeyResourceBuilder.build(context, auth, stack, resource);

        assertNotNull(result);
        assertEquals(resource.getName(), result.getName());

        verify(awsPublicKeyConnector, times(1)).register(any(PublicKeyRegisterRequest.class));
        verify(sshKeyNameGenerator, times(1)).getKeyPairName(auth, stack);
        verify(stack.getInstanceAuthentication(), times(1)).getPublicKey();
    }

}