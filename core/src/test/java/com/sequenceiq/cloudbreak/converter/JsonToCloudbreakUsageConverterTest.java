package com.sequenceiq.cloudbreak.converter;

import static java.util.Arrays.asList;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;

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
        assertAllFieldsNotNull(result,
                asList("day", "costs", "periodStarted", "duration", "status", "flexId", "stackUuid", "instanceNum", "peak", "parentUuid", "smartSenseId"));
    }

    @Override
    public Class<CloudbreakUsageJson> getRequestClass() {
        return CloudbreakUsageJson.class;
    }
}
