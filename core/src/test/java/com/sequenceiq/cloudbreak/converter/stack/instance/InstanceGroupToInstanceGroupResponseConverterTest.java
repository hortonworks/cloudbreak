package com.sequenceiq.cloudbreak.converter.stack.instance;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupToInstanceGroupV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

public class InstanceGroupToInstanceGroupResponseConverterTest extends AbstractEntityConverterTest<InstanceGroup> {

    @InjectMocks
    private InstanceGroupToInstanceGroupV4ResponseConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @Before
    public void setUp() {
        underTest = new InstanceGroupToInstanceGroupV4ResponseConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(converterUtil.convertAllAsSet(anySet(), eq(InstanceMetaDataV4Response.class))).willReturn(getInstanceMetaData(getSource()));
        // WHEN
        InstanceGroupV4Response result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, result.getNodeCount());
        assertEquals(InstanceGroupType.CORE, result.getType());
        assertAllFieldsNotNull(result, Lists.newArrayList("template", "securityGroup"));
    }

    @Override
    public InstanceGroup createSource() {
        InstanceGroup instanceGroup = TestUtil.instanceGroup(1L, InstanceGroupType.CORE, TestUtil.gcpTemplate(1L));
        instanceGroup.setInstanceMetaData(getInstanceMetaData(instanceGroup));
        return instanceGroup;
    }

    private Set<InstanceMetaData> getInstanceMetaData(InstanceGroup instanceGroup) {
        InstanceMetaData metadata = TestUtil.instanceMetaData(1L, 1L, InstanceStatus.REGISTERED, false, instanceGroup);
        return Sets.newHashSet(metadata);
    }
}
