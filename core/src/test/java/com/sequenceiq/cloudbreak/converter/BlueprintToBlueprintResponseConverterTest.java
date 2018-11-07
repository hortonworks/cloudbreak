package com.sequenceiq.cloudbreak.converter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import com.sequenceiq.cloudbreak.service.secret.SecretService;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintToBlueprintResponseConverterTest extends AbstractEntityConverterTest<Blueprint> {

    private static final JsonToString JSON_TO_STRING = new JsonToString();

    @InjectMocks
    private BlueprintToBlueprintResponseConverter underTest;

    @Mock
    private SecretService secretService;

    @Before
    public void setUp() {
        when(secretService.get(anyString())).thenAnswer(it -> it.getArgument(0));
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
        Assert.assertEquals(source.getBlueprintText(), result.getAmbariBlueprint());
    }

    @Override
    public Blueprint createSource() {
        return TestUtil.blueprint();
    }
}
