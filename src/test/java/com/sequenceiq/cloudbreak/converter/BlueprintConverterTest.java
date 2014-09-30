package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;

public class BlueprintConverterTest {

    public static final String DUMMY_NAME = "multi-node-hdfs-yarn";
    public static final String DUMMY_ID = "1";
    public static final String DUMMY_URL = "http://mycompany.com/#blueprint";
    public static final String DUMMY_DESCRIPTION = "dummyDescription";

    public static final String DUMMY_BLUEPRINT_TEXT =
            "{\"Blueprints\":{\"blueprint_name\":\"asd\"},\"host_groups\":[{\"name\":\"asd\"},{\"name\":\"slave_a\"}]}";

    public static final String DUMMY_BLUEPRINT_TEXT_WO_BLUEPRINTS =
            "{\"host_groups\":[{\"name\":\"asd\"},{\"name\":\"slave_a\"}]}";

    public static final String DUMMY_BLUEPRINT_TEXT_WO_HOST_GROUPS =
            "{\"Blueprints\":{\"blueprint_name\":\"asd\"}}";

    public static final String DUMMY_BLUEPRINT_TEXT_WO_BLUEPRINT_NAME =
            "{\"Blueprints\":{\"stack_name\":\"asd\"},\"host_groups\":[{\"name\":\"asd\"},{\"name\":\"slave_a\"}]}";

    public static final String DUMMY_BLUEPRINT_TEXT_HOSTGROUPS_NOT_ARRAY =
            "{\"Blueprints\":{\"stack_name\":\"asd\"},\"host_groups\":{\"name\":\"asd\"}}";

    public static final String DUMMY_BLUEPRINT_TEXT_HOSTGROUPS_DONT_HAVE_NAME =
            "{\"Blueprints\":{\"blueprint_name\":\"asd\"},\"host_groups\":[{\"names\":\"asd\"},{\"name\":\"slave_a\"}]}";

    public static final String DUMMY_BLUEPRINT_TEXT_HOSTGROUPS_DONT_HAVE_SLAVE =
            "{\"Blueprints\":{\"blueprint_name\":\"asd\"},\"host_groups\":[{\"name\":\"group1\"},{\"name\":\"group2\"}]}";

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
        assertEquals(result.getHostGroupCount(), 2);
    }

    @Test(expected = BadRequestException.class)
    public void testConvertBlueprintJsonToEntityShouldThrowBadRequestWhenBlueprintsIsNotInJson() {
        // GIVEN
        given(jsonNode.toString()).willReturn(DUMMY_BLUEPRINT_TEXT_WO_BLUEPRINTS);
        blueprintJson.setAmbariBlueprint(jsonNode);
        blueprintJson.setUrl(null);
        // WHEN
        Blueprint result = underTest.convert(blueprintJson);
        // THEN
        assertEquals(result.getBlueprintText(), blueprintJson.getAmbariBlueprint());
    }

    @Test(expected = BadRequestException.class)
    public void testConvertBlueprintJsonToEntityShouldThrowBadRequestWhenBlueprintNameIsNotInJson() {
        // GIVEN
        given(jsonNode.toString()).willReturn(DUMMY_BLUEPRINT_TEXT_WO_BLUEPRINT_NAME);
        blueprintJson.setAmbariBlueprint(jsonNode);
        blueprintJson.setUrl(null);
        // WHEN
        Blueprint result = underTest.convert(blueprintJson);
        // THEN
        assertEquals(result.getBlueprintText(), blueprintJson.getAmbariBlueprint());
    }

    @Test(expected = BadRequestException.class)
    public void testConvertBlueprintJsonToEntityShouldThrowBadRequestWhenHostGroupsIsNotInJson() {
        // GIVEN
        given(jsonNode.toString()).willReturn(DUMMY_BLUEPRINT_TEXT_WO_HOST_GROUPS);
        blueprintJson.setAmbariBlueprint(jsonNode);
        blueprintJson.setUrl(null);
        // WHEN
        Blueprint result = underTest.convert(blueprintJson);
        // THEN
        assertEquals(result.getBlueprintText(), blueprintJson.getAmbariBlueprint());
    }

    @Test(expected = BadRequestException.class)
    public void testConvertBlueprintJsonToEntityShouldThrowBadRequestWhenHostGroupsIsNotArray() {
        // GIVEN
        given(jsonNode.toString()).willReturn(DUMMY_BLUEPRINT_TEXT_HOSTGROUPS_NOT_ARRAY);
        blueprintJson.setAmbariBlueprint(jsonNode);
        blueprintJson.setUrl(null);
        // WHEN
        Blueprint result = underTest.convert(blueprintJson);
        // THEN
        assertEquals(result.getBlueprintText(), blueprintJson.getAmbariBlueprint());
    }

    @Test(expected = BadRequestException.class)
    public void testConvertBlueprintJsonToEntityShouldThrowBadRequestWhenHostGroupDoNotHaveName() {
        // GIVEN
        given(jsonNode.toString()).willReturn(DUMMY_BLUEPRINT_TEXT_HOSTGROUPS_DONT_HAVE_NAME);
        blueprintJson.setAmbariBlueprint(jsonNode);
        blueprintJson.setUrl(null);
        // WHEN
        Blueprint result = underTest.convert(blueprintJson);
        // THEN
        assertEquals(result.getBlueprintText(), blueprintJson.getAmbariBlueprint());
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
