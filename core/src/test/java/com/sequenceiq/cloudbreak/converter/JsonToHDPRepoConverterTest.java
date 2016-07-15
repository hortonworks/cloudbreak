package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;

public class JsonToHDPRepoConverterTest extends AbstractJsonConverterTest<AmbariStackDetailsJson> {
    private JsonToHDPRepoConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToHDPRepoConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        HDPRepo result = underTest.convert(getRequest("stack/ambari-stack-details.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<AmbariStackDetailsJson> getRequestClass() {
        return AmbariStackDetailsJson.class;
    }
}
