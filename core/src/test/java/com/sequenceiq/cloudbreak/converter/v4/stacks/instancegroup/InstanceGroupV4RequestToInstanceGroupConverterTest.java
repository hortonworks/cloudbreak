package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@ExtendWith(MockitoExtension.class)
public class InstanceGroupV4RequestToInstanceGroupConverterTest {

    @InjectMocks
    private InstanceGroupV4RequestToInstanceGroupConverter underTest;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private InstanceTemplateV4RequestToTemplateConverter instanceTemplateV4RequestToTemplateConverter;

    @Mock
    private SecurityGroupV4RequestToSecurityGroupConverter securityGroupV4RequestToSecurityGroupConverter;

    @Test
    public void testConvertWhenHostNameUpperCase() {
        InstanceGroupV4Request source = new InstanceGroupV4Request();
        source.setName("MixEdName");
        source.setTemplate(new InstanceTemplateV4Request());

        when(providerParameterCalculator.get(source)).thenReturn(mock(Mappable.class));
        when(instanceTemplateV4RequestToTemplateConverter.convert(any())).thenReturn(new Template());
        when(securityGroupV4RequestToSecurityGroupConverter.convert(any())).thenReturn(new SecurityGroup());

        InstanceGroup actual = underTest.convert(source);
        assertEquals(actual.getGroupName(), "mixedname");
    }
}
