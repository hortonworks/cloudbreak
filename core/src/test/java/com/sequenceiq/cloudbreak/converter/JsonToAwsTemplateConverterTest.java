package com.sequenceiq.cloudbreak.converter;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;

public class JsonToAwsTemplateConverterTest extends AbstractJsonConverterTest<TemplateRequest> {

    private JsonToAwsTemplateConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToAwsTemplateConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        AwsTemplate template = underTest.convert(getRequest("template/aws-template.json"));
        // THEN
        assertAllFieldsNotNull(template);
    }

    @Test
    public void testConvertInvalid() {
        // GIVEN
        // WHEN
        AwsTemplate template = underTest.convert(getRequest("template/aws-template-invalid.json"));
        // THEN
        assertAllFieldsNotNull(template, Arrays.asList("name"));
    }


    @Override
    public Class<TemplateRequest> getRequestClass() {
        return TemplateRequest.class;
    }
}
