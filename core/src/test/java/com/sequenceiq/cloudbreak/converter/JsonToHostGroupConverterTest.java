package com.sequenceiq.cloudbreak.converter;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.HostGroupJson;
import com.sequenceiq.cloudbreak.domain.HostGroup;

public class JsonToHostGroupConverterTest extends AbstractJsonConverterTest<HostGroupJson> {

    private JsonToHostGroupConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToHostGroupConverter();
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
    public Class<HostGroupJson> getRequestClass() {
        return HostGroupJson.class;
    }
}
