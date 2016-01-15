package com.sequenceiq.cloudbreak.converter;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;

public class JsonToCloudbreakUsageConverterTest extends AbstractJsonConverterTest<CloudbreakUsageJson> {

    private JsonToCloudbreakUsageConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToCloudbreakUsageConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        CloudbreakUsage result = underTest.convert(getRequest("usage/cloudbreak-usage.json"));
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("day", "costs"));
    }

    @Override
    public Class<CloudbreakUsageJson> getRequestClass() {
        return CloudbreakUsageJson.class;
    }
}
