package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.FailurePolicyResponse;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import org.junit.Before;
import org.junit.Test;

public class FailurePolicyRequestToFailurePolicyConverterTest extends AbstractJsonConverterTest<FailurePolicy> {

    private FailurePolicyToFailurePolicyResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new FailurePolicyToFailurePolicyResponseConverter();
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
