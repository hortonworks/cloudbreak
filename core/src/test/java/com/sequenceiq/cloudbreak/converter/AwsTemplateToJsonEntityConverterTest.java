package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import com.sequenceiq.cloudbreak.controller.validation.AwsTemplateParam;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;

public class AwsTemplateToJsonEntityConverterTest extends AbstractEntityConverterTest<AwsTemplate> {

    private AwsTemplateToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new AwsTemplateToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        TemplateResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(1.0, result.getParameters().get(AwsTemplateParam.SPOT_PRICE.getName()));
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertWithoutSpotPrice() {
        // GIVEN
        getSource().setSpotPrice(null);
        // WHEN
        TemplateResponse result = underTest.convert(getSource());
        // THEN
        assertNull(result.getParameters().get(AwsTemplateParam.SPOT_PRICE.getName()));
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
    public AwsTemplate createSource() {
        return (AwsTemplate) TestUtil.awsTemplate(1L);
    }
}
