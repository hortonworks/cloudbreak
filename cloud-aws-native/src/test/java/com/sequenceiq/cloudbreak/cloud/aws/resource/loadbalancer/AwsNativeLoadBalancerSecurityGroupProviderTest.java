package com.sequenceiq.cloudbreak.cloud.aws.resource.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.service.ResourceRetriever;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AwsNativeLoadBalancerSecurityGroupProviderTest {

    @InjectMocks
    private AwsNativeLoadBalancerSecurityGroupProvider securityGroupProvider;

    @Mock
    private ResourceRetriever resourceRetriever;

    @Mock
    private CloudStack stack;

    @Mock
    private Group group;

    @BeforeEach
    public void setUp() {
        when(stack.getGroups()).thenReturn(Collections.singletonList(group));
    }

    @Test
    void testGetSecurityGroupsWithExistingSecurityGroupIds() {
        Long stackId = 12345L;
        Security cloudSecurity = mock(Security.class);
        when(cloudSecurity.getCloudSecurityIds()).thenReturn(Arrays.asList("sg-123", "sg-456"));
        when(group.getSecurity()).thenReturn(cloudSecurity);
        when(group.getType()).thenReturn(InstanceGroupType.GATEWAY);

        List<String> result = securityGroupProvider.getSecurityGroups(stackId, stack);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("sg-123"));
        assertTrue(result.contains("sg-456"));

        verify(resourceRetriever, never()).findAllByStatusAndTypeAndStack(any(), any(), any());
    }

    @Test
    void testGetSecurityGroupsNoExistingSecurityGroupIds() {
        Long stackId = 12345L;

        Security cloudSecurity = mock(Security.class);
        when(cloudSecurity.getCloudSecurityIds()).thenReturn(Collections.emptyList());
        when(group.getSecurity()).thenReturn(cloudSecurity);
        when(group.getType()).thenReturn(InstanceGroupType.GATEWAY);
        when(group.getName()).thenReturn("gatewayGroup");

        CloudResource resource = mock(CloudResource.class);
        when(resource.getGroup()).thenReturn("gatewayGroup");
        when(resource.getReference()).thenReturn("sg-789");

        when(resourceRetriever.findAllByStatusAndTypeAndStack(any(), any(), anyLong()))
                .thenReturn(Collections.singletonList(resource));

        List<String> result = securityGroupProvider.getSecurityGroups(stackId, stack);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains("sg-789"));

        verify(resourceRetriever).findAllByStatusAndTypeAndStack(eq(CommonStatus.CREATED), eq(ResourceType.AWS_SECURITY_GROUP), eq(stackId));
    }
}