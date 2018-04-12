package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.HostGroupRequest;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

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
        HostGroup result = underTest.convert(getRequest("stack/host-group.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cluster", "instanceGroup", "constraint"));
    }

    @Override
    public Class<HostGroupRequest> getRequestClass() {
        return HostGroupRequest.class;
    }
}
