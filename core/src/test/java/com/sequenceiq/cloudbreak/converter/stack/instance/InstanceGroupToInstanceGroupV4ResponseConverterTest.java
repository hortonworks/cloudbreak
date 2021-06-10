package com.sequenceiq.cloudbreak.converter.stack.instance;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupToInstanceGroupV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@RunWith(MockitoJUnitRunner.class)
public class InstanceGroupToInstanceGroupV4ResponseConverterTest extends AbstractEntityConverterTest<InstanceGroup> {

    @InjectMocks
    private InstanceGroupToInstanceGroupV4ResponseConverter underTest;

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private ConversionService conversionService;

    @Test
    public void testConvert() {
        InstanceGroup source = getSource();
        when(converterUtil.convertAllAsSet(anySet(), eq(InstanceMetaDataV4Response.class))).thenReturn(getInstanceMetaData(source));

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

    private Set<InstanceMetaDataV4Response> getInstanceMetaData(InstanceGroup instanceGroup) {
        var metaDataV4Response = instanceGroup.getInstanceMetaDataSet().stream().map(instanceMetaData -> {
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
        }).collect(Collectors.toSet());
        return Sets.newHashSet(metaDataV4Response);
    }

}
