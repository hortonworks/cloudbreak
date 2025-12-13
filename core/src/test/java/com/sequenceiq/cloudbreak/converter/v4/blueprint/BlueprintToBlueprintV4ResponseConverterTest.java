package com.sequenceiq.cloudbreak.converter.v4.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.domain.Blueprint;

class BlueprintToBlueprintV4ResponseConverterTest extends AbstractEntityConverterTest<Blueprint> {

    private static final JsonToString JSON_TO_STRING = new JsonToString();

    private BlueprintToBlueprintV4ResponseConverter underTest = new BlueprintToBlueprintV4ResponseConverter();

    @Test
    void testConvert() {
        BlueprintV4Response result = underTest.convert(createSource());
        assertAllFieldsNotNull(result, List.of("created"));
    }

    @Test
    void testConvertContainsEmptyMapInTagsProperty() {
        Blueprint source = createSource();
        source.setTags(new Json(""));
        BlueprintV4Response result = underTest.convert(source);
        assertTrue(result.getTags().isEmpty());
    }

    @Test
    void testConvertContainsExpectedSingleKeyValuePairInTagsProperty() {
        String key = "name";
        String name = "greg";
        Blueprint source = createSource();
        source.setTags(new JsonToString().convertToEntityAttribute(String.format("{\"%s\":\"%s\"}", key, name)));

        BlueprintV4Response result = underTest.convert(source);

        assertNotNull(result.getTags());
        assertTrue(result.getTags().containsKey(key));
        assertNotNull(result.getTags().get(key));
        assertEquals(name, result.getTags().get(key));
    }

    @Test
    void testConvertContainsExpectedMultipleKeyValuePairInTagsProperty() {
        String nameKey = "name";
        String nameValue = "test";
        String ageKey = "address";
        String ageValue = "something else";
        Blueprint source = createSource();
        source.setTags(JSON_TO_STRING.convertToEntityAttribute(String.format("{\"%s\":\"%s\", \"%s\":\"%s\"}", nameKey, nameValue, ageKey, ageValue)));

        BlueprintV4Response result = underTest.convert(source);

        assertNotNull(result.getTags());
        assertTrue(result.getTags().containsKey(nameKey));
        assertTrue(result.getTags().containsKey(ageKey));
        assertNotNull(result.getTags().get(nameKey));
        assertNotNull(result.getTags().get(ageKey));
        assertEquals(nameValue, result.getTags().get(nameKey));
        assertEquals(ageValue, result.getTags().get(ageKey));
    }

    @Test
    void testConvertContainsExpectedMultipleKeyValuePairWhenItsTypesAreDifferentInTagsProperty() {
        String nameKey = "name";
        String nameValue = "test";
        String intKey = "number";
        Integer intValue = 11;
        Blueprint source = createSource();
        source.setTags(JSON_TO_STRING.convertToEntityAttribute(String.format("{\"%s\":\"%s\", \"%s\":%d}", nameKey, nameValue, intKey, intValue)));

        BlueprintV4Response result = underTest.convert(source);

        assertNotNull(result.getTags());
        assertTrue(result.getTags().containsKey(nameKey));
        assertTrue(result.getTags().containsKey(intKey));
        assertNotNull(result.getTags().get(nameKey));
        assertNotNull(result.getTags().get(intKey));
        assertEquals(nameValue, result.getTags().get(nameKey));
        assertEquals(intValue, result.getTags().get(intKey));
    }

    @Test
    void testConvertWhereEveryDataHasTransferredCorrectlyToResponseAndEntityDescriptionIsNullThenResultDescriptionShouldBeEmpty() {
        Blueprint source = createSource();
        source.setDescription(null);
        source.setTags(JSON_TO_STRING.convertToEntityAttribute("{}"));

        BlueprintV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(source.getResourceCrn(), result.getCrn());
        assertEquals(source.getName(), result.getName());
        assertNotNull(result.getDescription());
        assertTrue(result.getDescription().isEmpty());
        assertEquals(Integer.valueOf(source.getHostGroupCount()), result.getHostGroupCount());
        assertEquals(source.getStatus(), result.getStatus());
        assertNotNull(result.getTags());
        assertTrue(result.getTags().isEmpty());
        assertEquals(source.getBlueprintJsonText(), result.getBlueprint());
    }

    @Test
    void testConvertWhereEveryDataHasTransferredCorrectlyToResponseAndEntityDescriptionIsNotNullThenResultDescriptionShouldBeEmpty() {
        Blueprint source = createSource();
        source.setDescription("some description");
        source.setTags(JSON_TO_STRING.convertToEntityAttribute("{}"));

        BlueprintV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(source.getResourceCrn(), result.getCrn());
        assertEquals(source.getName(), result.getName());
        assertEquals(source.getDescription(), result.getDescription());
        assertEquals(Integer.valueOf(source.getHostGroupCount()), result.getHostGroupCount());
        assertEquals(source.getStatus(), result.getStatus());
        assertNotNull(result.getTags());
        assertTrue(result.getTags().isEmpty());
        assertEquals(source.getBlueprintJsonText(), result.getBlueprint());
    }

    @Override
    public Blueprint createSource() {
        return TestUtil.blueprint();
    }
}
