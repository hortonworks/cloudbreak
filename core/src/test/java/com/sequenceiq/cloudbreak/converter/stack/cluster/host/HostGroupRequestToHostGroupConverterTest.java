package com.sequenceiq.cloudbreak.converter.stack.cluster.host;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostGroupRequest;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

public class HostGroupRequestToHostGroupConverterTest extends AbstractJsonConverterTest<HostGroupRequest> {

    private HostGroupRequestToHostGroupConverter underTest;

    @Before
    public void setUp() {
        underTest = new HostGroupRequestToHostGroupConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        HostGroup result = underTest.convert(getRequest("host-group.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cluster", "instanceGroup", "constraint"));
    }

    @Override
    public Class<HostGroupRequest> getRequestClass() {
        return HostGroupRequest.class;
    }
}
