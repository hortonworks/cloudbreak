package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.CloudbreakUsageJson;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;

public class CloudbreakUsageRequestToCloudbreakUsageConverterTest extends AbstractJsonConverterTest<CloudbreakUsageJson> {

    private CloudbreakUsageRequestToCloudbreakUsageConverter underTest;

    @Before
    public void setUp() {
        underTest = new CloudbreakUsageRequestToCloudbreakUsageConverter();
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
