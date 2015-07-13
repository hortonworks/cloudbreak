package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import com.sequenceiq.cloudbreak.controller.validation.OpenStackTemplateParam;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;

public class OpenStackTemplateToJsonConverterTest extends AbstractEntityConverterTest<OpenStackTemplate> {

    private OpenStackTemplateToJsonConverter underTest;

    @Before
    public void setUp() {
        underTest = new OpenStackTemplateToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        TemplateResponse result = underTest.convert(getSource());
        // THEN
        assertEquals((Integer) 100, result.getVolumeSize());
        assertEquals("Big", result.getParameters().get(OpenStackTemplateParam.INSTANCE_TYPE.getName()));
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
    public OpenStackTemplate createSource() {
        return (OpenStackTemplate) TestUtil.openstackTemplate(1L);
    }
}
