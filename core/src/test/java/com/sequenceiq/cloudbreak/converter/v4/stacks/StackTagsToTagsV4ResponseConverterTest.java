package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.tags.TagsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.common.api.tag.model.Tags;

public class StackTagsToTagsV4ResponseConverterTest {

    private StackTagsToTagsV4ResponseConverter underTest;

    @Before
    public void setUp() {
        underTest = new StackTagsToTagsV4ResponseConverter();
    }

    @Test
    public void testConvert() {
        Tags applicationTags = new Tags(Map.of("aTagK1", "aTagV1", "aTagK2", "aTagV2"));
        Tags defaultTags = new Tags(Map.of("dTagK1", "dTagV1", "dTagK2", "dTagV2"));
        Tags userDefinedTags = new Tags(Map.of("uTagK1", "uTagV1", "uTagK2", "uTagV2"));
        StackTags source = new StackTags(userDefinedTags, applicationTags, defaultTags);

        TagsV4Response result = underTest.convert(source);

        assertNotNull(result);
        assertEquals(applicationTags.getAll(), result.getApplication().getAll());
        assertEquals(defaultTags.getAll(), result.getDefaults().getAll());
        assertEquals(userDefinedTags.getAll(), result.getUserDefined().getAll());
    }

}