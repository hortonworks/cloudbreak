package com.sequenceiq.cloudbreak.converter.v4.stacks.updates;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

@ExtendWith(MockitoExtension.class)
public class HostGroupV4RequestToHostGroupConverterTest {

    @InjectMocks
    private HostGroupV4RequestToHostGroupConverter underTest;

    @Test
    public void testConvertWhenHostNameUpperCase() {
        HostGroupV4Request source = new HostGroupV4Request();
        source.setName("MixEdName");

        HostGroup actual = underTest.convert(source);
        assertEquals(actual.getName(), "mixedname");
    }
}
