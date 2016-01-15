package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson;
import com.sequenceiq.cloudbreak.api.model.AdjustmentType;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;

public class FailurePolicyToJsonConverterTest extends AbstractEntityConverterTest<FailurePolicy> {

    private FailurePolicyToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new FailurePolicyToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        FailurePolicyJson result = underTest.convert(getSource());
        // THEN
        assertEquals(AdjustmentType.BEST_EFFORT, result.getAdjustmentType());
        assertAllFieldsNotNull(result);
    }

    @Override
    public FailurePolicy createSource() {
        return TestUtil.failurePolicy();
    }
}
