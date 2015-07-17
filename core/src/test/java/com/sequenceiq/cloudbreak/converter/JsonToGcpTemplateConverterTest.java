package com.sequenceiq.cloudbreak.converter;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;
import com.sequenceiq.cloudbreak.domain.GcpTemplate;

public class JsonToGcpTemplateConverterTest extends AbstractJsonConverterTest<TemplateRequest> {

    private JsonToGcpTemplateConverter underTest;

    @Before
    public void setUp() {
        underTest = new JsonToGcpTemplateConverter();
    }

    @Test
    public void testConvert() {
        // GIVEN
        // WHEN
        GcpTemplate result = underTest.convert(getRequest("template/gcp-template.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertWithoutVolumeCount() {
        // GIVEN
        // WHEN
        GcpTemplate result = underTest.convert(getRequest("template/gcp-template-without-volumecount.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertWithoutVolumeSize() {
        // GIVEN
        // WHEN
        GcpTemplate result = underTest.convert(getRequest("template/gcp-template-without-volumesize.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertWithoutVolumeType() {
        // GIVEN
        // WHEN
        GcpTemplate result = underTest.convert(getRequest("template/gcp-template-without-volumetype.json"));
        // THEN
        assertAllFieldsNotNull(result);
    }

    @Override
    public Class<TemplateRequest> getRequestClass() {
        return TemplateRequest.class;
    }
}
