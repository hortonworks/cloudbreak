package com.sequenceiq.cloudbreak.converter.stack.cluster.host;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.updates.HostGroupV4RequestToHostGroupConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;

class HostGroupRequestToHostGroupConverterTest extends AbstractJsonConverterTest<HostGroupV4Request> {

    private HostGroupV4RequestToHostGroupConverter underTest = new HostGroupV4RequestToHostGroupConverter();

    @Test
    void testConvert() {
        HostGroup result = underTest.convert(getRequest("host-group.json"));
        assertAllFieldsNotNull(result, Arrays.asList("cluster", "instanceGroup"));
    }

    @Override
    public Class<HostGroupV4Request> getRequestClass() {
        return HostGroupV4Request.class;
    }
}
