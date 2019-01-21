package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.AdjustmentType;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyResponse;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;

public class FailurePolicyToFailurePolicyResponseConverterTest extends AbstractEntityConverterTest<FailurePolicy> {

    private FailurePolicyToFailurePolicyResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new FailurePolicyToFailurePolicyResponseConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        FailurePolicyResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(AdjustmentType.BEST_EFFORT, result.getAdjustmentType());
        assertAllFieldsNotNull(result);
    }

    @Override
    public FailurePolicy createSource() {
        return TestUtil.failurePolicy();
    }
}
