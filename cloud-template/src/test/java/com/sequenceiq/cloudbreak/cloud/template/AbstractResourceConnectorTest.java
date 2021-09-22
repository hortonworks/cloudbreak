package com.sequenceiq.cloudbreak.cloud.template;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.TemplatingNotSupportedException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.TlsInfo;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class AbstractResourceConnectorTest {

    private AbstractResourceConnector underTest;

    @Mock
    private Group group;

    @Mock
    private ResourceBuilderContext context;

    @BeforeEach
    public void setup() {
        underTest = new AbstractResourceConnector() {
            @Override
            protected List<CloudResource> collectProviderSpecificResources(List<CloudResource> resources, List<CloudInstance> vms) {
                return null;
            }

            @Override
            public List<CloudResourceStatus> launchLoadBalancers(AuthenticatedContext authenticatedContext, CloudStack stack,
                    PersistenceNotifier persistenceNotifier) throws Exception {
                return null;
            }

            @Override
            public void updateUserData(AuthenticatedContext authenticatedContext, CloudStack stack, List<CloudResource> resources, String userData) {

            }

            @Override
            public TlsInfo getTlsInfo(AuthenticatedContext authenticatedContext, CloudStack cloudStack) {
                return null;
            }

            @Override
            public String getStackTemplate() throws TemplatingNotSupportedException {
                return null;
            }

            @Override
            public String getDBStackTemplate() throws TemplatingNotSupportedException {
                return null;
            }

            @Override
            protected ResourceType getDiskResourceType() {
                return ResourceType.AWS_VOLUMESET;
            }
        };
    }

    @Test
    public void testDiskReattachmentWhenNoResourceWithDiffGroup() {
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.getGroup()).thenReturn("any");
        when(group.getName()).thenReturn("groupName");
        underTest.diskReattachment(List.of(cloudResource), group, context);
        verify(cloudResource, never()).getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        verify(cloudResource, never()).getType();
    }

    @Test
    public void testDiskReattachmentWhenNoResourceWithDiffDiskResourceType() {
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.getGroup()).thenReturn("groupName");
        when(cloudResource.getType()).thenReturn(ResourceType.AWS_INSTANCE);
        when(group.getName()).thenReturn("groupName");
        underTest.diskReattachment(List.of(cloudResource), group, context);
        verify(cloudResource, never()).getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        verify(cloudResource, never()).getInstanceId();
    }

    @Test
    public void testDiskReattachmentWhenInstanceNotEmptyAndCreated() {
        CloudResource cloudResource = mock(CloudResource.class);
        when(cloudResource.getGroup()).thenReturn("groupName");
        when(cloudResource.getType()).thenReturn(ResourceType.AWS_VOLUMESET);
        when(cloudResource.getInstanceId()).thenReturn("instanceId");
        when(cloudResource.getStatus()).thenReturn(CommonStatus.CREATED);
        when(group.getName()).thenReturn("groupName");
        underTest.diskReattachment(List.of(cloudResource), group, context);
        verify(cloudResource, never()).getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
    }

    @Test
    public void testDiskReattachmentWhenInstanceEmptyButNoInstanceWithHostGroup() {
        CloudResource cloudResource = mock(CloudResource.class);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        when(cloudResource.getGroup()).thenReturn("groupName");
        when(cloudResource.getType()).thenReturn(ResourceType.AWS_VOLUMESET);
        when(cloudResource.getInstanceId()).thenReturn(null);
        when(cloudInstance.getStringParameter(CloudInstance.FQDN)).thenReturn("any-fqdn");
        when(group.getName()).thenReturn("groupName");
        when(group.getInstances()).thenReturn(List.of(cloudInstance));
        when(cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class)).thenReturn(volumeSetAttributes);
        when(volumeSetAttributes.getDiscoveryFQDN()).thenReturn("fqdn");

        underTest.diskReattachment(List.of(cloudResource), group, context);
        verify(context, never()).addComputeResources(any(), any());
    }

    @Test
    public void testDiskReattachmentWhenResourceDetachedAndHostEquals() {
        CloudResource cloudResource = mock(CloudResource.class);
        CloudInstance cloudInstance = mock(CloudInstance.class);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        VolumeSetAttributes volumeSetAttributes = mock(VolumeSetAttributes.class);
        when(cloudResource.getGroup()).thenReturn("groupName");
        when(cloudResource.getType()).thenReturn(ResourceType.AWS_VOLUMESET);
        when(cloudResource.getInstanceId()).thenReturn("any");
        when(cloudResource.getStatus()).thenReturn(CommonStatus.DETACHED);
        when(cloudInstance.getStringParameter(CloudInstance.FQDN)).thenReturn("fqdn");
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);
        when(instanceTemplate.getPrivateId()).thenReturn(1L);
        when(group.getName()).thenReturn("groupName");
        when(group.getInstances()).thenReturn(List.of(cloudInstance));
        when(cloudResource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class)).thenReturn(volumeSetAttributes);
        when(volumeSetAttributes.getDiscoveryFQDN()).thenReturn("fqdn");

        underTest.diskReattachment(List.of(cloudResource), group, context);
        verify(context).addComputeResources(1L, List.of(cloudResource));
    }
}
