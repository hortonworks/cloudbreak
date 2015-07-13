package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;

public class AmbariStackDetailsToJsonEntityConverterTest extends AbstractEntityConverterTest<AmbariStackDetails> {

    private AmbariStackDetailsToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new AmbariStackDetailsToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        AmbariStackDetailsJson result = underTest.convert(getSource());
        // THEN
        assertEquals("dummyOs", result.getOs());
        assertAllFieldsNotNull(result);
    }

    @Override
    public AmbariStackDetails createSource() {
        return TestUtil.ambariStackDetails();
    }
}
