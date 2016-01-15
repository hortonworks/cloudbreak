package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;

public class JsonToFailurePolicyConverterTest extends AbstractJsonConverterTest<FailurePolicyJson> {

    private JsonToFailurePolicyConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToFailurePolicyConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        FailurePolicy result = underTest.convert(getRequest("stack/failure-policy.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertWithoutThreshold() {
        // GIVEN
        // WHEN
        FailurePolicy result = underTest.convert(getRequest("stack/failure-policy-without-threshold.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<FailurePolicyJson> getRequestClass() {
        return FailurePolicyJson.class;
    }
}
