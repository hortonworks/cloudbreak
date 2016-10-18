package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.FailurePolicyResponse;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;

public class JsonToFailurePolicyConverterTest extends AbstractJsonConverterTest<FailurePolicy> {

    private FailurePolicyToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new FailurePolicyToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        FailurePolicyResponse result = underTest.convert(getRequest("stack/failure-policy.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertWithoutThreshold() {
        // GIVEN
        // WHEN
        FailurePolicyResponse result = underTest.convert(getRequest("stack/failure-policy-without-threshold.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<FailurePolicy> getRequestClass() {
        return FailurePolicy.class;
    }
}
