package com.sequenceiq.cloudbreak.converter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.BlueprintParameterJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

public class BlueprintToBlueprintResponseConverterTest extends AbstractEntityConverterTest<Blueprint> {

    private static final JsonToString JSON_TO_STRING = new JsonToString();

    private BlueprintToBlueprintResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new BlueprintToBlueprintResponseConverter();
    }

    @Test
    public void testConvert() {
        BlueprintResponse result = underTest.convert(createSource());
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertContainsEmptyMapInTagsProperty() throws JsonProcessingException {
        Blueprint source = createSource();
        source.setTags(new Json(""));
        BlueprintResponse result = underTest.convert(source);
        Assert.assertTrue(result.getTags().isEmpty());
    }

    @Test
    public void testConvertContainsExpectedSingleKeyValuePairInTagsProperty() {
        String key = "name";
        String name = "greg";
        Blueprint source = createSource();
        source.setTags(new JsonToString().convertToEntityAttribute(String.format("{\"%s\":\"%s\"}", key, name)));

        BlueprintResponse result = underTest.convert(source);

        Assert.assertNotNull(result.getTags());
        Assert.assertTrue(result.getTags().containsKey(key));
        Assert.assertNotNull(result.getTags().get(key));
        Assert.assertEquals(name, result.getTags().get(key));
    }

    @Test
    public void testConvertContainsExpectedMultipleKeyValuePairInTagsProperty() {
        String nameKey = "name";
        String nameValue = "test";
        String ageKey = "address";
        String ageValue = "something else";
        Blueprint source = createSource();
        source.setTags(JSON_TO_STRING.convertToEntityAttribute(String.format("{\"%s\":\"%s\", \"%s\":\"%s\"}", nameKey, nameValue, ageKey, ageValue)));

        BlueprintResponse result = underTest.convert(source);

        Assert.assertNotNull(result.getTags());
        Assert.assertTrue(result.getTags().containsKey(nameKey));
        Assert.assertTrue(result.getTags().containsKey(ageKey));
        Assert.assertNotNull(result.getTags().get(nameKey));
        Assert.assertNotNull(result.getTags().get(ageKey));
        Assert.assertEquals(nameValue, result.getTags().get(nameKey));
        Assert.assertEquals(ageValue, result.getTags().get(ageKey));
    }

    @Test
    public void testConvertContainsExpectedMultipleKeyValuePairWhenItsTypesAreDifferentInTagsProperty() {
        String nameKey = "name";
        String nameValue = "test";
        String intKey = "number";
        Integer intValue = 11;
        Blueprint source = createSource();
        source.setTags(JSON_TO_STRING.convertToEntityAttribute(String.format("{\"%s\":\"%s\", \"%s\":%d}", nameKey, nameValue, intKey, intValue)));

        BlueprintResponse result = underTest.convert(source);

        Assert.assertNotNull(result.getTags());
        Assert.assertTrue(result.getTags().containsKey(nameKey));
        Assert.assertTrue(result.getTags().containsKey(intKey));
        Assert.assertNotNull(result.getTags().get(nameKey));
        Assert.assertNotNull(result.getTags().get(intKey));
        Assert.assertEquals(nameValue, result.getTags().get(nameKey));
        Assert.assertEquals(intValue, result.getTags().get(intKey));
    }

    @Test
    public void testConvertWhereEveryDataHasTransferredCorrectlyToResponseAndEntityDescriptionIsNullThenResultDescriptionShouldBeEmpty() {
        Blueprint source = createSource();
        source.setDescription(null);
        source.setTags(JSON_TO_STRING.convertToEntityAttribute("{}"));

        BlueprintResponse result = underTest.convert(source);

        Assert.assertNotNull(result);
        Assert.assertEquals(source.getId(), result.getId());
        Assert.assertEquals(source.getName(), result.getName());
        Assert.assertNotNull(result.getDescription());
        Assert.assertTrue(result.getDescription().isEmpty());
        Assert.assertEquals(Integer.valueOf(source.getHostGroupCount()), result.getHostGroupCount());
        Assert.assertEquals(source.getStatus(), result.getStatus());
        Assert.assertNotNull(result.getTags());
        Assert.assertTrue(result.getTags().isEmpty());
        Assert.assertNotNull(result.getInputs());
        Assert.assertTrue(result.getInputs().isEmpty());
        Assert.assertEquals(source.getBlueprintText(), result.getAmbariBlueprint());
    }

    @Test
    public void testConvertWhereEveryDataHasTransferredCorrectlyToResponseAndEntityDescriptionIsNotNullThenResultDescriptionShouldBeEmpty() {
        Blueprint source = createSource();
        source.setDescription("some description");
        source.setTags(JSON_TO_STRING.convertToEntityAttribute("{}"));

        BlueprintResponse result = underTest.convert(source);

        Assert.assertNotNull(result);
        Assert.assertEquals(source.getId(), result.getId());
        Assert.assertEquals(source.getName(), result.getName());
        Assert.assertEquals(source.getDescription(), result.getDescription());
        Assert.assertEquals(Integer.valueOf(source.getHostGroupCount()), result.getHostGroupCount());
        Assert.assertEquals(source.getStatus(), result.getStatus());
        Assert.assertNotNull(result.getTags());
        Assert.assertTrue(result.getTags().isEmpty());
        Assert.assertNotNull(result.getInputs());
        Assert.assertTrue(result.getInputs().isEmpty());
        Assert.assertEquals(source.getBlueprintText(), result.getAmbariBlueprint());
    }

    @Test
    public void testConvertWhenInputParametersContainsValidDataThenItShouldBeInTheResponse() {
        Blueprint source = createSource();
        String nameKey = "name";
        String nameValue = "some name";
        String descriptionKey = "description";
        String descriptionValue = "some description";
        String referenceConfigurationKey = "referenceConfiguration";
        String referenceConfigurationValue = "some data";
        source.setInputParameters(JSON_TO_STRING.convertToEntityAttribute(String.format("{\"parameters\":[{\"%s\":\"%s\",\"%s\":\"%s\",\"%s\":\"%s\"}]}",
                nameKey, nameValue, descriptionKey, descriptionValue, referenceConfigurationKey, referenceConfigurationValue)));

        BlueprintResponse result = underTest.convert(source);

        Assert.assertFalse(result.getInputs().isEmpty());
        Assert.assertEquals(1L, result.getInputs().size());
        for (BlueprintParameterJson blueprintParameterJson : result.getInputs()) {
            Assert.assertEquals(nameValue, blueprintParameterJson.getName());
            Assert.assertEquals(descriptionValue, blueprintParameterJson.getDescription());
            Assert.assertEquals(referenceConfigurationValue, blueprintParameterJson.getReferenceConfiguration());
        }
    }

    @Test
    public void testConvertWhenEntityInputParameterContainsEmptyValueWichIsNotSuitableToConvertToBlueprintInputParametersThenInputsInResultShouldBeEmpty() {
        Blueprint source = createSource();
        source.setInputParameters(JSON_TO_STRING.convertToEntityAttribute("{}"));

        BlueprintResponse result = underTest.convert(source);

        Assert.assertTrue(result.getInputs().isEmpty());
    }

    @Test
    public void testConvertWhenEntityInputParameterContainsValueWichIsNotSuitableToConvertToBlueprintInputParametersThenInputsInResultShouldBeEmpty() {
        Blueprint source = createSource();
        source.setInputParameters(JSON_TO_STRING.convertToEntityAttribute("{\"some key which should not be here\":\"whatever\"}"));

        BlueprintResponse result = underTest.convert(source);

        Assert.assertTrue(result.getInputs().isEmpty());
    }

    @Override
    public Blueprint createSource() {
        return TestUtil.blueprint();
    }
}
