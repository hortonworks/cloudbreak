package com.sequenceiq.cloudbreak.core.flow2.stack.migration.handler.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.resource.group.AwsSecurityGroupResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class SecurityGroupRecreatorServiceTest {

    @InjectMocks
    private SecurityGroupRecreatorService underTest;

    @Mock
    private PersistenceNotifier persistenceNotifier;

    @Mock
    private AwsSecurityGroupResourceBuilder awsSecurityGroupResourceBuilder;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AwsContext awsContext;

    @Mock
    private Network network;

    @Mock
    private Group group;

    @Mock
    private ResourcePersisted resourcePersisted;

    @Test
    public void testAcceptWhenPersistSuccess() throws Exception {
        String hostGroupName = "hostGroupName";
        CreateResourcesRequest request = new CreateResourcesRequest(cloudContext, cloudCredential, cloudStack, hostGroupName);
        CloudResource cloudResource = CloudResource.builder()
                .type(ResourceType.AWS_SECURITY_GROUP)
                .status(CommonStatus.CREATED)
                .name("name")
                .params(Collections.emptyMap())
                .build();

        when(group.getName()).thenReturn(hostGroupName);
        when(cloudStack.getGroups()).thenReturn(List.of(group));
        when(cloudStack.getNetwork()).thenReturn(network);
        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), any())).thenReturn(resourcePersisted);
        when(awsSecurityGroupResourceBuilder.create(awsContext, ac, group, network)).thenReturn(cloudResource);
        when(awsSecurityGroupResourceBuilder.build(eq(awsContext), eq(ac), eq(group), eq(network), any(), any())).thenReturn(cloudResource);

        underTest.recreate(request, awsContext, ac);

        verify(awsSecurityGroupResourceBuilder).build(any(), any(), any(), any(), any(), any());

        verify(awsSecurityGroupResourceBuilder).create(awsContext, ac, group, network);
        verify(persistenceNotifier).notifyUpdate(cloudResource, cloudContext);
    }

    @Test
    public void testAcceptWhenPersistNotSuccess() throws Exception {
        String hostGroupName = "hostGroupName";
        CreateResourcesRequest request = new CreateResourcesRequest(cloudContext, cloudCredential, cloudStack, hostGroupName);
        CloudResource cloudResource = CloudResource.builder()
                .type(ResourceType.AWS_SECURITY_GROUP)
                .status(CommonStatus.CREATED)
                .name("name")
                .params(Collections.emptyMap())
                .build();

        when(group.getName()).thenReturn(hostGroupName);
        when(cloudStack.getGroups()).thenReturn(List.of(group));
        when(cloudStack.getNetwork()).thenReturn(network);
        Exception value = new Exception();
        when(resourcePersisted.getException()).thenReturn(value);
        when(persistenceNotifier.notifyAllocation(any(CloudResource.class), any())).thenReturn(resourcePersisted);
        when(awsSecurityGroupResourceBuilder.create(awsContext, ac, group, network)).thenReturn(cloudResource);

        Exception actual = Assertions.assertThrows(Exception.class, () -> underTest.recreate(request, awsContext, ac));

        Assertions.assertEquals(value, actual);

        verify(awsSecurityGroupResourceBuilder, never()).build(any(), any(), any(), any(), any(), any());
        verify(awsSecurityGroupResourceBuilder).create(awsContext, ac, group, network);
    }
}
