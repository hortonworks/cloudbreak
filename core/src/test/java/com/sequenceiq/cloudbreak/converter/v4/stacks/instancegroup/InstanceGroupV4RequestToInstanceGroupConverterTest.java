package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;

@ExtendWith(MockitoExtension.class)
public class InstanceGroupV4RequestToInstanceGroupConverterTest {

    @InjectMocks
    private InstanceGroupV4RequestToInstanceGroupConverter underTest;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private ConversionService conversionService;

    @Test
    public void testConvertWhenHostNameUpperCase() {
        InstanceGroupV4Request source = new InstanceGroupV4Request();
        source.setName("MixEdName");
        source.setTemplate(new InstanceTemplateV4Request());

        when(providerParameterCalculator.get(source)).thenReturn(mock(Mappable.class));

        InstanceGroup actual = underTest.convert(source);
        assertEquals(actual.getGroupName(), "mixedname");
    }
}
