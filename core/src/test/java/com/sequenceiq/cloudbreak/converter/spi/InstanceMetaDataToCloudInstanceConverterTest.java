package com.sequenceiq.cloudbreak.converter.spi;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToImageIdConverter;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.common.api.type.InstanceGroupType;

@RunWith(MockitoJUnitRunner.class)
public class InstanceMetaDataToCloudInstanceConverterTest extends AbstractEntityConverterTest<InstanceMetaData> {

    private static final String SUBNET_ID = "SUBNET_ID";

    private static final String INSTANCE_NAME = "INSTANCE_NAME";

    @Mock
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Mock
    private InstanceMetadataToImageIdConverter instanceMetadataToImageIdConverter;

    @InjectMocks
    private InstanceMetaDataToCloudInstanceConverter underTest;

    @Test
    public void testConvert() {
        InstanceMetaData source = getSource();
        Map<String, Object> params = new HashMap<>();
        params.put(CloudInstance.SUBNET_ID, SUBNET_ID);
        params.put(CloudInstance.INSTANCE_NAME, INSTANCE_NAME);
        when(stackToCloudStackConverter.buildCloudInstanceParameters(any(), any(), any())).thenReturn(params);
        CloudInstance cloudInstance = underTest.convert(source, "envCrn", new StackAuthentication());

        assertEquals(2, cloudInstance.getParameters().size());
        assertEquals(source.getInstanceId(), cloudInstance.getInstanceId());
    }

    @Override
    public InstanceMetaData createSource() {
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, TestUtil.gcpTemplate(1L));
        Stack stack = TestUtil.stack();
        stack.setStackAuthentication(new StackAuthentication());
        instanceGroup.setStack(stack);
        return TestUtil.instanceMetaData(1L, 1L, InstanceStatus.SERVICES_RUNNING, false, instanceGroup);
    }

}