package com.sequenceiq.cloudbreak.shell.util;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class TagParserTest {
    @Test
    public void testNullTags() {
        Map<String, String> result = TagParser.parseTagsIntoMap(null);
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testEmptyTags() {
        Map<String, String> result = TagParser.parseTagsIntoMap("");
        Assert.assertEquals(0, result.size());
    }

    @Test
    public void testOneTag() {
        Map<String, String> result = TagParser.parseTagsIntoMap("key=value");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("value", result.get("key"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongTag() {
        TagParser.parseTagsIntoMap("key");
    }

    @Test
    public void testMoreTags() {
        Map<String, String> result = TagParser.parseTagsIntoMap("key1=value1,key2=value2,key3=value3");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("value1", result.get("key1"));
        Assert.assertEquals("value2", result.get("key2"));
        Assert.assertEquals("value3", result.get("key3"));
    }

    @Test
    public void testMoreAndSpacesTags() {
        Map<String, String> result = TagParser.parseTagsIntoMap("ke  y1=val ue1,  key2=value2  , key3=value3");
        Assert.assertEquals(3, result.size());
        Assert.assertEquals("val ue1", result.get("ke  y1"));
        Assert.assertEquals("value2", result.get("key2"));
        Assert.assertEquals("value3", result.get("key3"));
    }
}
