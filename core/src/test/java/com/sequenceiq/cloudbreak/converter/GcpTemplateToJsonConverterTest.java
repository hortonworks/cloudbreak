package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import com.sequenceiq.cloudbreak.controller.validation.GcpTemplateParam;
import com.sequenceiq.cloudbreak.domain.GcpTemplate;

public class GcpTemplateToJsonConverterTest extends AbstractEntityConverterTest<GcpTemplate> {

    private static final String N1_HIGHCPU_16 = "n1-highcpu-16";
    private GcpTemplateToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new GcpTemplateToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        TemplateResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(N1_HIGHCPU_16, result.getParameters().get(GcpTemplateParam.INSTANCETYPE.getName()));
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertWithoutDescription() {
        // GIVEN
        getSource().setDescription(null);
        // WHEN
        TemplateResponse result = underTest.convert(getSource());
        // THEN
        assertEquals("", result.getDescription());
        assertAllFieldsNotNull(result);
    }

    @Override
    public GcpTemplate createSource() {
        return (GcpTemplate) TestUtil.gcpTemplate(1L);
    }
}
