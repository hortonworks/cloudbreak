package com.sequenceiq.cloudbreak.converter;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;

public class JsonToStackRepoDetailsConverterTest extends AbstractJsonConverterTest<AmbariStackDetailsJson> {
    private JsonToHDPRepoConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToHDPRepoConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        StackRepoDetails result = underTest.convert(getRequest("stack/ambari-stack-details.json"));
        // THEN
        assertAllFieldsNotNull(result, Collections.singletonList("knox"));
    }

    @Override
    public Class<AmbariStackDetailsJson> getRequestClass() {
        return AmbariStackDetailsJson.class;
    }
}
