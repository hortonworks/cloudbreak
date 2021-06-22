package com.sequenceiq.cloudbreak.converter.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.InstanceMetadataToImageIdConverter;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class InstanceMetaDataToCloudInstanceConverterTest extends AbstractEntityConverterTest<InstanceMetaData> {

    private static final String SUBNET_ID = "SUBNET_ID";

    private static final String SUBNET_ID_2 = "SUBNET_ID_2";

    private static final String INSTANCE_NAME = "INSTANCE_NAME";

    private static final String INSTANCE_NAME_2 = "INSTANCE_NAME_2";

    private static final String AVAILABILITY_ZONE = "AVAILABILITY_ZONE";

    private static final String AVAILABILITY_ZONE_2 = "AVAILABILITY_ZONE_2";

    private static final String ENV_CRN = "envCrn";

    @Mock
    private StackToCloudStackConverter stackToCloudStackConverter;

    @Mock
    private InstanceMetadataToImageIdConverter instanceMetadataToImageIdConverter;

    @InjectMocks
    private InstanceMetaDataToCloudInstanceConverter underTest;

    @Test
    public void testConvertWhenParamsFromCloudInstanceOnly() {
        InstanceMetaData source = getSource();
        initStackToCloudStackConverter(true);

        CloudInstance cloudInstance = underTest.convert(source, ENV_CRN, new StackAuthentication());

        verifyParams(cloudInstance, SUBNET_ID, INSTANCE_NAME, AVAILABILITY_ZONE);
        assertEquals(source.getInstanceId(), cloudInstance.getInstanceId());
    }

    private void verifyParams(CloudInstance cloudInstance, String subnetId, String instanceName, String availabilityZone) {
        assertEquals(2, cloudInstance.getParameters().size());
        assertThat(cloudInstance.getStringParameter(NetworkConstants.SUBNET_ID)).isEqualTo(subnetId);
        assertThat(cloudInstance.getStringParameter(CloudInstance.INSTANCE_NAME)).isEqualTo(instanceName);
    }

    private void initStackToCloudStackConverter(boolean withParams) {
        Map<String, Object> params = new HashMap<>();
        if (withParams) {
            params.put(NetworkConstants.SUBNET_ID, SUBNET_ID);
            params.put(CloudInstance.INSTANCE_NAME, INSTANCE_NAME);
        }
        when(stackToCloudStackConverter.buildCloudInstanceParameters(any(), any(), any())).thenReturn(params);
    }

    @Test
    public void testConvertWhenParamsFromInstanceMetaDataAreOverriddenByCloudInstance() {
        InstanceMetaData source = getSource();
        addParamsToInstanceMetaData(source);
        initStackToCloudStackConverter(true);

        CloudInstance cloudInstance = underTest.convert(source, ENV_CRN, new StackAuthentication());

        verifyParams(cloudInstance, SUBNET_ID, INSTANCE_NAME, AVAILABILITY_ZONE);
        assertEquals(source.getInstanceId(), cloudInstance.getInstanceId());
    }

    private void addParamsToInstanceMetaData(InstanceMetaData instanceMetaData) {
        instanceMetaData.setSubnetId(SUBNET_ID_2);
        instanceMetaData.setInstanceName(INSTANCE_NAME_2);
        instanceMetaData.setAvailabilityZone(AVAILABILITY_ZONE_2);
    }

    @Test
    public void testConvertWhenParamsFromInstanceMetaDataOnly() {
        InstanceMetaData source = getSource();
        addParamsToInstanceMetaData(source);
        initStackToCloudStackConverter(false);

        CloudInstance cloudInstance = underTest.convert(source, ENV_CRN, new StackAuthentication());

        verifyParams(cloudInstance, SUBNET_ID_2, INSTANCE_NAME_2, AVAILABILITY_ZONE_2);
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