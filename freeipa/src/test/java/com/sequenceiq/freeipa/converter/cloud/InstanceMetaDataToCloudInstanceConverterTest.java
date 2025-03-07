package com.sequenceiq.freeipa.converter.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.freeipa.entity.ImageEntity;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackAuthentication;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.entity.projection.StackAuthenticationView;
import com.sequenceiq.freeipa.service.image.ImageService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@ExtendWith(MockitoExtension.class)
public class InstanceMetaDataToCloudInstanceConverterTest {

    private static final Long INSTANCE_METADATA_ID = 1L;

    private static final String INSTANCE_AZ = "instanceAz";

    private static final Long STACK_ID = 1L;

    private static final String IMAGE_ID = "image";

    private static final String ENV_CRN = "envCrn";

    private static final InstanceStatus INSTANCE_STATUS = InstanceStatus.CREATED;

    @Mock
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Mock
    private ImageService imageService;

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private InstanceMetaData instanceMetaData;

    @InjectMocks
    private InstanceMetaDataToCloudInstanceConverter underTest;

    @BeforeEach
    public void before() {
        Template template = mock(Template.class);
        InstanceGroup group = mock(InstanceGroup.class);
        when(group.getGroupName()).thenReturn("group");
        when(group.getTemplate()).thenReturn(template);
        when(instanceMetaData.getId()).thenReturn(INSTANCE_METADATA_ID);
        when(instanceMetaData.getPrivateId()).thenReturn(INSTANCE_METADATA_ID);
        when(instanceMetaData.getInstanceGroup()).thenReturn(group);
        when(instanceMetaData.getInstanceStatus()).thenReturn(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus.CREATED);
        when(instanceMetaData.getAvailabilityZone()).thenReturn(INSTANCE_AZ);

        StackAuthentication stackAuthentication = mock(StackAuthentication.class);
        StackAuthenticationView stackAuthenticationView = mock(StackAuthenticationView.class);
        when(stackAuthenticationView.getStackId()).thenReturn(STACK_ID);
        ImageEntity imageEntity = mock(ImageEntity.class);
        when(imageEntity.getImageName()).thenReturn(IMAGE_ID);
        when(imageService.getByStackId(STACK_ID)).thenReturn(imageEntity);
        when(stackAuthenticationView.getStackAuthentication()).thenReturn(stackAuthentication);
        when(instanceMetaDataService.getStackAuthenticationViewByInstanceMetaDataId(INSTANCE_METADATA_ID)).thenReturn(Optional.of(stackAuthenticationView));
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(stackToCloudStackConverter.buildInstanceTemplate(template, "group", INSTANCE_METADATA_ID, INSTANCE_STATUS, IMAGE_ID))
                .thenReturn(instanceTemplate);
        Stack stack = mock(Stack.class);
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        when(stackToCloudStackConverter.buildCloudInstanceParameters(ENV_CRN, instanceMetaData)).thenReturn(Map.of());
    }

    @Test
    void testConvert() {
        CloudInstance cloudInstance = underTest.convert(instanceMetaData);
        assertEquals(INSTANCE_AZ, cloudInstance.getAvailabilityZone());
    }

}
