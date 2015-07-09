package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import com.sequenceiq.cloudbreak.controller.validation.GcpTemplateParam;
import com.sequenceiq.cloudbreak.domain.GcpInstanceType;
import com.sequenceiq.cloudbreak.domain.GcpTemplate;

public class GcpTemplateToJsonEntityConverterTest extends AbstractEntityConverterTest<GcpTemplate> {

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
        assertEquals(GcpInstanceType.N1_HIGHCPU_16.name(), result.getParameters().get(GcpTemplateParam.INSTANCETYPE.getName()));
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
