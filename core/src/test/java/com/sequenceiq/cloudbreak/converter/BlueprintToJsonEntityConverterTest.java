package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.BlueprintResponse;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;

public class BlueprintToJsonEntityConverterTest extends AbstractEntityConverterTest<Blueprint> {

    @InjectMocks
    private BlueprintToJsonConverter underTest;

    @Mock
    private JsonHelper jsonHelper;

    @Mock
    private JsonNode jsonNode;

    @Before
    public void setUp() {
        underTest = new BlueprintToJsonConverter();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(jsonHelper.createJsonFromString(anyString())).willReturn(jsonNode);
        given(jsonNode.toString()).willReturn("dummyAmbariBlueprint");
        // WHEN
        BlueprintResponse result = underTest.convert(getSource());
        // THEN
        assertEquals("multi-node-yarn", result.getBlueprintName());
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertThrowsException() {
        // GIVEN
        given(jsonHelper.createJsonFromString(anyString())).willThrow(new RuntimeException("error"));
        // WHEN
        BlueprintResponse result = underTest.convert(getSource());
        // THEN
        assertEquals("\"error\"", result.getAmbariBlueprint());
        assertAllFieldsNotNull(result);
    }

    @Override
    public Blueprint createSource() {
        return TestUtil.blueprint();
    }
}
