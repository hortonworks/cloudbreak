package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.SecurityGroupV4RequestToSecurityGroupConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.InstanceTemplateV4RequestToTemplateConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
class InstanceGroupV4RequestToInstanceGroupConverterTest {

    @InjectMocks
    private InstanceGroupV4RequestToInstanceGroupConverter underTest;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private InstanceTemplateV4RequestToTemplateConverter instanceTemplateV4RequestToTemplateConverter;

    @Mock
    private SecurityGroupV4RequestToSecurityGroupConverter securityGroupV4RequestToSecurityGroupConverter;

    @BeforeEach
    void setUp() {
        lenient().when(providerParameterCalculator.get(any())).thenReturn(mock(Mappable.class));
    }

    @Test
    void testConvertWhenHostNameUpperCase() {
        InstanceGroupV4Request source = new InstanceGroupV4Request();
        source.setName("MixEdName");
        source.setTemplate(new InstanceTemplateV4Request());
        source.setType(InstanceGroupType.GATEWAY);

        when(providerParameterCalculator.get(source)).thenReturn(mock(Mappable.class));
        when(instanceTemplateV4RequestToTemplateConverter.convert(any(), eq(true))).thenReturn(new Template());

        InstanceGroup actual = underTest.convert(source, "variant");
        assertEquals(actual.getGroupName(), "mixedname");
    }

    @Test
    void testConvertProviderInstanceType() {
        InstanceGroupV4Request source = new InstanceGroupV4Request();
        source.setName("name");
        source.setTemplate(new InstanceTemplateV4Request());
        source.setType(InstanceGroupType.GATEWAY);
        source.setNodeCount(2);
        Template template = new Template();
        template.setInstanceType("large");
        when(instanceTemplateV4RequestToTemplateConverter.convert(any(), eq(true))).thenReturn(template);

        InstanceGroup actual = underTest.convert(source, "variant");
        assertEquals(2, actual.getInstanceMetaData().size());
        assertThat(actual.getInstanceMetaData()).extracting(InstanceMetaData::getProviderInstanceType).containsOnly("large");
    }
}
