package com.sequenceiq.cloudbreak.converter.stack.cluster.host;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.updates.HostGroupV4RequestToHostGroupConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

public class HostGroupRequestToHostGroupConverterTest extends AbstractJsonConverterTest<HostGroupV4Request> {

    private HostGroupV4RequestToHostGroupConverter underTest;

    @Before
    public void setUp() {
        underTest = new HostGroupV4RequestToHostGroupConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        HostGroup result = underTest.convert(getRequest("host-group.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cluster", "instanceGroup"));
    }

    @Override
    public Class<HostGroupV4Request> getRequestClass() {
        return HostGroupV4Request.class;
    }
}
