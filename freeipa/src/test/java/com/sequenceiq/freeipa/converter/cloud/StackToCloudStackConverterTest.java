package com.sequenceiq.freeipa.converter.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.freeipa.converter.image.ImageConverter;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackAuthentication;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;
import com.sequenceiq.freeipa.service.SecurityRuleService;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.image.ImageService;

public class StackToCloudStackConverterTest {

    private static final Long TEST_STACK_ID = 1L;

    private static final Integer VOLUME_COUNT = 0;

    private static final String INSTANCE_ID = "instance-id";

    private static final String GROUP_NAME = "group-name";

    private static final String IMAGE_NAME = "image-name";

    private static final String ENV_CRN = "env-crn";

    @InjectMocks
    private StackToCloudStackConverter underTest;

    @Mock
    private Stack stack;

    @Mock
    private StackAuthentication stackAuthentication;

    @Mock
    private SecurityRuleService securityRuleService;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Mock
    private ImageService imageService;

    @Mock
    private ImageConverter imageConverter;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
    }

    @Test
    public void testBuildInstance() throws Exception {
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        ImageEntity imageEntity = mock(ImageEntity.class);
        when(imageService.getByStack(any())).thenReturn(imageEntity);
        when(imageEntity.getImageName()).thenReturn(IMAGE_NAME);
        when(instanceMetaData.getInstanceId()).thenReturn(INSTANCE_ID);
        when(instanceGroup.getGroupName()).thenReturn(GROUP_NAME);
        Template template = mock(Template.class);
        when(instanceGroup.getTemplate()).thenReturn(template);
        when(template.getVolumeCount()).thenReturn(VOLUME_COUNT);
        CloudInstance cloudInstance = underTest.buildInstance(stack, instanceMetaData, instanceGroup, stackAuthentication, 0L, InstanceStatus.CREATED);
        assertEquals(INSTANCE_ID, cloudInstance.getInstanceId());
    }
}