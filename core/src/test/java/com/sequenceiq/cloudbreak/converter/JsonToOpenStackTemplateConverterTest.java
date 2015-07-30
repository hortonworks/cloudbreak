package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;

public class JsonToOpenStackTemplateConverterTest extends AbstractJsonConverterTest<TemplateRequest> {
    private JsonToOpenStackTemplateConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToOpenStackTemplateConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        OpenStackTemplate result = underTest.convert(getRequest("template/openstack-template.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<TemplateRequest> getRequestClass() {
        return TemplateRequest.class;
    }
}
