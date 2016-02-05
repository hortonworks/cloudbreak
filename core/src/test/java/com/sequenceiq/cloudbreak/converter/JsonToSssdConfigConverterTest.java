package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest;
import com.sequenceiq.cloudbreak.domain.SssdConfig;

public class JsonToSssdConfigConverterTest extends AbstractJsonConverterTest<SssdConfigRequest> {

    private JsonToSssdConfigConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToSssdConfigConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        SssdConfig result = underTest.convert(getRequest("stack/sssd_config.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<SssdConfigRequest> getRequestClass() {
        return SssdConfigRequest.class;
    }
}
