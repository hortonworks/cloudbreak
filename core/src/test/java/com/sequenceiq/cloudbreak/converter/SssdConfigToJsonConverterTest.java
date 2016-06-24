package com.sequenceiq.cloudbreak.converter;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse;
import com.sequenceiq.cloudbreak.domain.SssdConfig;

public class SssdConfigToJsonConverterTest extends AbstractEntityConverterTest<SssdConfig> {

    private SssdConfigToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new SssdConfigToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        SssdConfigResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Collections.singletonList("id"));
    }

    @Override
    public SssdConfig createSource() {
        return TestUtil.sssdConfigs(1).iterator().next();
    }
}
