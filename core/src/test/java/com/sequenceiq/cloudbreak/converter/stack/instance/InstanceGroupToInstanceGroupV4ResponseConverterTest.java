package com.sequenceiq.cloudbreak.converter.stack.instance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupToInstanceGroupV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceMetaDataToInstanceMetaDataV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.network.InstanceGroupNetworkToInstanceGroupNetworkV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.SecurityGroupToSecurityGroupResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.TemplateToInstanceTemplateV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class InstanceGroupToInstanceGroupV4ResponseConverterTest extends AbstractEntityConverterTest<InstanceGroup> {

    @InjectMocks
    private InstanceGroupToInstanceGroupV4ResponseConverter underTest;

    @Mock
    private TemplateToInstanceTemplateV4ResponseConverter templateToInstanceTemplateV4ResponseConverter;

    @Mock
    private InstanceGroupNetworkToInstanceGroupNetworkV4ResponseConverter instanceGroupNetworkToInstanceGroupNetworkV4ResponseConverter;

    @Mock
    private SecurityGroupToSecurityGroupResponseConverter securityGroupToSecurityGroupResponseConverter;

    @Mock
    private InstanceMetaDataToInstanceMetaDataV4ResponseConverter instanceMetaDataToInstanceMetaDataV4ResponseConverter;

    @Test
    void testConvert() {
        InstanceGroup source = getSource();
        for (InstanceMetaData allInstanceMetaDatum : source.getAllInstanceMetaData()) {
            when(instanceMetaDataToInstanceMetaDataV4ResponseConverter.convert(any()))
                    .thenReturn(getInstanceMetaData(allInstanceMetaDatum));
        }

        InstanceGroupV4Response result = underTest.convert(getSource());

        assertEquals(source.getNodeCount(), result.getNodeCount());
        assertEquals(getSource().getInstanceGroupType(), result.getType());
        assertAllFieldsNotNull(result, Lists.newArrayList("recipes", "template", "network",
                "securityGroup", "gcp", "name", "mock", "openstack",
                "aws", "yarn", "azure"));
    }

    @Override
    public InstanceGroup createSource() {
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, TestUtil.gcpTemplate(1L));
        InstanceMetaData metaData = TestUtil.instanceMetaData(1L, 1L, InstanceStatus.SERVICES_RUNNING, false, instanceGroup);
        instanceGroup.setInstanceMetaData(Set.of(metaData));
        return instanceGroup;
    }

    private InstanceMetaDataV4Response getInstanceMetaData(InstanceMetaData instanceMetaData) {
        InstanceMetaDataV4Response response = new InstanceMetaDataV4Response();
        response.setPublicIp(instanceMetaData.getPublicIp());
        response.setAmbariServer(instanceMetaData.getAmbariServer());
        response.setDiscoveryFQDN(instanceMetaData.getDiscoveryFQDN());
        response.setInstanceGroup(instanceMetaData.getInstanceGroupName());
        response.setInstanceStatus(instanceMetaData.getInstanceStatus());
        response.setInstanceId(instanceMetaData.getInstanceId());
        response.setInstanceType(instanceMetaData.getInstanceMetadataType());
        response.setPrivateIp(instanceMetaData.getPrivateIp());
        response.setSshPort(instanceMetaData.getSshPort());
        return response;
    }

}
