package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import com.sequenceiq.cloudbreak.controller.validation.AzureTemplateParam;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.AzureVmType;

public class AzureTemplateToJsonEntityConverterTest extends AbstractEntityConverterTest<AzureTemplate> {

    private AzureTemplateToJsonConverter underTest = new AzureTemplateToJsonConverter();

    public void setUp() {
        underTest = new AzureTemplateToJsonConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        TemplateResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(AzureVmType.A5.toString(), result.getParameters().get(AzureTemplateParam.VMTYPE.getName()));
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
    public AzureTemplate createSource() {
        return (AzureTemplate) TestUtil.azureTemplate(1L);
    }
}
