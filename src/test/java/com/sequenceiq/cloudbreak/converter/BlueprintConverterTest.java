package com.sequenceiq.cloudbreak.converter;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.times;
import static org.mockito.Matchers.anyString;
import static org.junit.Assert.assertEquals;

public class BlueprintConverterTest {

    public static final String DUMMY_NAME = "multi-node-hdfs-yarn";
    public static final String DUMMY_ID = "1";
    public static final String DUMMY_URL = "http://mycompany.com/#blueprint";
    public static final String DUMMY_DESCRIPTION = "dummyDescription";
    public static final String DUMMY_BLUEPRINT_TEXT = "\"blueprint_name\" : \"multi-node-hdfs-yarn\",";
    public static final String ERROR_MSG = "msg";
    @InjectMocks
    private BlueprintConverter underTest;

    @Mock
    private JsonHelper jsonHelper;

    @Mock
    private JsonNode jsonNode;

    private Blueprint blueprint;

    private BlueprintJson blueprintJson;

    @Before
    public void setUp() {
        underTest = new BlueprintConverter();
        MockitoAnnotations.initMocks(this);
        blueprint = createBlueprint();
        blueprintJson = createBlueprintJson();
    }

    @Test
    public void testConvertBlueprintEntityToJson() {
        // GIVEN
        given(jsonHelper.createJsonFromString(anyString())).willReturn(jsonNode);
        // WHEN
        underTest.convert(blueprint);
        // THEN
        verify(jsonHelper, times(1)).createJsonFromString(anyString());
    }

    @Test
    public void testConvertBlueprintEntityToJsonWhenCouldNotConvertJson() {
        // GIVEN
        given(jsonHelper.createJsonFromString(anyString())).willThrow(new IllegalStateException(ERROR_MSG));
        // WHEN
        BlueprintJson blueprintJson = underTest.convert(blueprint);
        // THEN
        assertEquals(blueprintJson.getAmbariBlueprint(), "\"" + ERROR_MSG + "\"");
    }

    @Test
    public void testConvertBlueprintJsonToEntity() {
        // GIVEN
        given(jsonNode.toString()).willReturn(DUMMY_BLUEPRINT_TEXT);
        blueprintJson.setAmbariBlueprint(jsonNode);
        blueprintJson.setUrl(null);
        // WHEN
        Blueprint result = underTest.convert(blueprintJson);
        // THEN
        assertEquals(result.getBlueprintText(), blueprintJson.getAmbariBlueprint());
    }

    @Test(expected = BadRequestException.class)
    public void testConvertBlueprintJsonToEntityWhenPatternNotMatch() {
        // GIVEN
        // WHEN
        underTest.convert(blueprintJson);
    }

    private BlueprintJson createBlueprintJson() {
        BlueprintJson blueprintJson = new BlueprintJson();
        blueprintJson.setBlueprintName(DUMMY_NAME);
        blueprintJson.setId(DUMMY_ID);
        blueprintJson.setAmbariBlueprint(jsonNode);
        blueprintJson.setUrl(DUMMY_URL);
        blueprintJson.setDescription(DUMMY_DESCRIPTION);
        return blueprintJson;
    }

    private Blueprint createBlueprint() {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintName(DUMMY_NAME);
        blueprint.setDescription(DUMMY_DESCRIPTION);
        blueprint.setId(Long.parseLong(DUMMY_ID));
        blueprint.setBlueprintText(DUMMY_BLUEPRINT_TEXT);
        return blueprint;
    }

}
