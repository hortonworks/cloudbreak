package com.sequenceiq.cloudbreak.converter.stack.instance;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupToInstanceGroupV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

public class InstanceGroupToInstanceGroupResponseConverterTest extends AbstractEntityConverterTest<InstanceGroup> {

    @InjectMocks
    private InstanceGroupToInstanceGroupV4ResponseConverter underTest;

    @Mock
    private ConverterUtil converterUtil;

    @Before
    public void setUp() {
        underTest = new InstanceGroupToInstanceGroupV4ResponseConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        var source = getSource();
        when(converterUtil.convertAllAsSet(anySet(), eq(InstanceMetaDataV4Response.class))).thenReturn(getInstanceMetaData(source));

        InstanceGroupV4Response result = underTest.convert(getSource());

        assertEquals(source.getNodeCount(), result.getNodeCount());
        assertEquals(getSource().getInstanceGroupType(), result.getType());
        assertAllFieldsNotNull(result, Lists.newArrayList("template", "securityGroup"));
    }

    @Override
    public InstanceGroup createSource() {
        var instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, TestUtil.gcpTemplate(1L));
        var metaData = TestUtil.instanceMetaData(1L, 1L, InstanceStatus.REGISTERED, false, instanceGroup);
        instanceGroup.setInstanceMetaData(Set.of(metaData));
        return instanceGroup;
    }

    private Set<InstanceMetaDataV4Response> getInstanceMetaData(InstanceGroup instanceGroup) {
        var metaDataV4Response = instanceGroup.getInstanceMetaDataSet().stream().map(instanceMetaData -> {
            var response = new InstanceMetaDataV4Response();
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
