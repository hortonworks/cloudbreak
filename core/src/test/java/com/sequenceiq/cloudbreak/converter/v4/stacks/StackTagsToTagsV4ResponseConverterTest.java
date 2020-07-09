package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;

public class StackTagsToTagsV4ResponseConverterTest {

    private StackTagsToTagsV4ResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new StackTagsToTagsV4ResponseConverter();
    }

    @Test
    public void testConvert() {
        Map<String, String> applicationTags = Map.of("aTagK1", "aTagV1", "aTagK2", "aTagV2");
        Map<String, String> defaultTags = Map.of("dTagK1", "dTagV1", "dTagK2", "dTagV2");
        Map<String, String> userDefinedTags = Map.of("uTagK1", "uTagV1", "uTagK2", "uTagV2");
        StackTags source = new StackTags(userDefinedTags, applicationTags, defaultTags);

        TagsV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(applicationTags, result.getApplication());
        assertEquals(defaultTags, result.getDefaults());
        assertEquals(userDefinedTags, result.getUserDefined());
    }

}