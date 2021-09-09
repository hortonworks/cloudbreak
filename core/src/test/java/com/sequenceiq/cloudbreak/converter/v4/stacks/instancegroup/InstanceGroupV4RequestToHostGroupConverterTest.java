package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

@ExtendWith(MockitoExtension.class)
public class InstanceGroupV4RequestToHostGroupConverterTest {

    @InjectMocks
    private InstanceGroupV4RequestToHostGroupConverter underTest;

    @Test
    public void testConvertWhenHostNameUpperCase() {
        InstanceGroupV4Request source = new InstanceGroupV4Request();
        source.setName("MixEdName");
        source.setTemplate(new InstanceTemplateV4Request());

        HostGroup actual = underTest.convert(source);
        assertEquals(actual.getName(), "mixedname");
    }
}
