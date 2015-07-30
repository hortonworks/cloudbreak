package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.json.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;

public class JsonToAmbariStackDetailsConverterTest extends AbstractJsonConverterTest<AmbariStackDetailsJson> {

    private JsonToAmbariStackDetailsConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToAmbariStackDetailsConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        AmbariStackDetails result = underTest.convert(getRequest("stack/ambari-stack-details.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<AmbariStackDetailsJson> getRequestClass() {
        return AmbariStackDetailsJson.class;
    }
}
