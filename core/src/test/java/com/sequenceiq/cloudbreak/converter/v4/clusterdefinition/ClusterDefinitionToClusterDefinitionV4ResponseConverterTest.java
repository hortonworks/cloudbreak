package com.sequenceiq.cloudbreak.converter.v4.clusterdefinition;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

public class ClusterDefinitionToClusterDefinitionV4ResponseConverterTest extends AbstractEntityConverterTest<ClusterDefinition> {

    private static final JsonToString JSON_TO_STRING = new JsonToString();

    private ClusterDefinitionToClusterDefinitionV4ResponseConverter underTest = new ClusterDefinitionToClusterDefinitionV4ResponseConverter();

    @Test
    public void testConvert() {
        ClusterDefinitionV4Response result = underTest.convert(createSource());
        assertAllFieldsNotNull(result);
    }

    @Test
    public void testConvertContainsEmptyMapInTagsProperty() {
        ClusterDefinition source = createSource();
        source.setTags(new Json(""));
        ClusterDefinitionV4Response result = underTest.convert(source);
        Assert.assertTrue(result.getTags().isEmpty());
    }

    @Test
    public void testConvertContainsExpectedSingleKeyValuePairInTagsProperty() {
        String key = "name";
        String name = "greg";
        ClusterDefinition source = createSource();
        source.setTags(new JsonToString().convertToEntityAttribute(String.format("{\"%s\":\"%s\"}", key, name)));

        ClusterDefinitionV4Response result = underTest.convert(source);

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
        ClusterDefinition source = createSource();
        source.setTags(JSON_TO_STRING.convertToEntityAttribute(String.format("{\"%s\":\"%s\", \"%s\":\"%s\"}", nameKey, nameValue, ageKey, ageValue)));

        ClusterDefinitionV4Response result = underTest.convert(source);

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
        ClusterDefinition source = createSource();
        source.setTags(JSON_TO_STRING.convertToEntityAttribute(String.format("{\"%s\":\"%s\", \"%s\":%d}", nameKey, nameValue, intKey, intValue)));

        ClusterDefinitionV4Response result = underTest.convert(source);

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
        ClusterDefinition source = createSource();
        source.setDescription(null);
        source.setTags(JSON_TO_STRING.convertToEntityAttribute("{}"));

        ClusterDefinitionV4Response result = underTest.convert(source);

        Assert.assertNotNull(result);
        Assert.assertEquals(source.getId(), result.getId());
        Assert.assertEquals(source.getName(), result.getName());
        Assert.assertNotNull(result.getDescription());
        Assert.assertTrue(result.getDescription().isEmpty());
        Assert.assertEquals(Integer.valueOf(source.getHostGroupCount()), result.getHostGroupCount());
        Assert.assertEquals(source.getStatus(), result.getStatus());
        Assert.assertNotNull(result.getTags());
        Assert.assertTrue(result.getTags().isEmpty());
        Assert.assertEquals(source.getClusterDefinitionText(), result.getClusterDefinition());
    }

    @Test
    public void testConvertWhereEveryDataHasTransferredCorrectlyToResponseAndEntityDescriptionIsNotNullThenResultDescriptionShouldBeEmpty() {
        ClusterDefinition source = createSource();
        source.setDescription("some description");
        source.setTags(JSON_TO_STRING.convertToEntityAttribute("{}"));

        ClusterDefinitionV4Response result = underTest.convert(source);

        Assert.assertNotNull(result);
        Assert.assertEquals(source.getId(), result.getId());
        Assert.assertEquals(source.getName(), result.getName());
        Assert.assertEquals(source.getDescription(), result.getDescription());
        Assert.assertEquals(Integer.valueOf(source.getHostGroupCount()), result.getHostGroupCount());
        Assert.assertEquals(source.getStatus(), result.getStatus());
        Assert.assertNotNull(result.getTags());
        Assert.assertTrue(result.getTags().isEmpty());
        Assert.assertEquals(source.getClusterDefinitionText(), result.getClusterDefinition());
    }

    @Override
    public ClusterDefinition createSource() {
        return TestUtil.clusterDefinition();
    }
}
