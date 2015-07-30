package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;

public class JsonToAzureTemplateConverterTest extends AbstractJsonConverterTest<TemplateRequest> {

    private JsonToAzureTemplateConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToAzureTemplateConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        AzureTemplate result = underTest.convert(getRequest("template/azure-template.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<TemplateRequest> getRequestClass() {
        return TemplateRequest.class;
    }
}
