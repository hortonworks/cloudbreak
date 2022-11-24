package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.aws.AwsImageUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsLaunchTemplateUpdateService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.ResourceType;

public class AwsUpdateServiceTest {

    @Mock
    private AwsImageUpdateService awsImageUpdateService;

    @Mock
    private CloudStack stack;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AwsLaunchTemplateUpdateService awsLaunchTemplateUpdateService;

    @InjectMocks
    private AwsUpdateService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void updateCloudFormationTemplateResourceWithImageParameter() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withParams(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();

        List<CloudResourceStatus> statuses = underTest.update(ac, stack, Collections.singletonList(cloudResource), UpdateType.IMAGE_UPDATE);

        verify(awsImageUpdateService, times(1)).updateImage(ac, stack, cloudResource);
        assertEquals(1, statuses.size());
        assertEquals(ResourceStatus.UPDATED, statuses.get(0).getStatus());
        assertEquals(cloudResource, statuses.get(0).getCloudResource());
    }

    @Test
    public void updateCloudFormationTemplateResourceWithoutImageParameter() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .build();
        CloudResource launch = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.AWS_LAUNCHCONFIGURATION)
                .build();
        when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchTemplate");
        doNothing().when(awsLaunchTemplateUpdateService).updateLaunchTemplate(anyMap(), any(), anyString(), any());

        List<CloudResourceStatus> statuses = underTest.update(ac, stack, List.of(cloudResource, launch), UpdateType.IMAGE_UPDATE);

        verify(awsImageUpdateService, times(0)).updateImage(ac, stack, cloudResource);
        assertEquals(0, statuses.size());
    }

    @Test
    public void updateRandomResource() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.AWS_LAUNCHCONFIGURATION)
                .withParams(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();
        CloudResource cf = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withParams(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();
        doNothing().when(awsLaunchTemplateUpdateService).updateLaunchTemplate(anyMap(), any(), anyString(), any());

        List<CloudResourceStatus> statuses = underTest.update(ac, stack, List.of(cloudResource, cf), UpdateType.IMAGE_UPDATE);

        verify(awsImageUpdateService, times(0)).updateImage(ac, stack, cloudResource);
        assertEquals(1, statuses.size());
    }
}
