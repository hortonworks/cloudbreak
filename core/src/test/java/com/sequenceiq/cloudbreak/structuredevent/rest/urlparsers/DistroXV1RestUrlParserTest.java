package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.regex.Matcher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DistroXV1RestUrlParserTest {

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameter(1)
    public boolean match;

    @Parameterized.Parameter(2)
    public String resourceCrn;

    @Parameterized.Parameter(3)
    public String resourceName;

    @Parameterized.Parameter(4)
    public String resourceType;

    @Parameterized.Parameter(5)
    public String resourceEvent;

    private DistroXV1RestUrlParser underTest;

    @Before
    public void init() {
        underTest = new DistroXV1RestUrlParser();
    }

    @Test
    public void testMatch() {
        Matcher matcher = underTest.getPattern().matcher(url);
        if (match) {
            assertTrue(matcher.matches());
            assertEquals(resourceName, underTest.getResourceName(matcher));
            assertEquals(resourceCrn, underTest.getResourceCrn(matcher));
            assertEquals(resourceType, underTest.getResourceType(matcher));
        } else {
            assertFalse(matcher.matches());
        }
    }

    @Parameterized.Parameters(name
            = "{index}: testMatch(url: {0}, should match {1}, resourceCrn: {2}, resourceName: {3}, resourceType: {4}, resourceEvent: {5}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"v1/distrox/name/asdf", true, null, "asdf", "stacks", null},
                {"v1/distrox/random/asdf", false, null, null, "stacks", null},
                {"v1/distrox/name/asdf/event", true, null, "asdf", "stacks", "event"},
                {"v1/name/asdf", false, null, null, "stacks", null},
                {"distrox", false, null, null, "stacks", null},
                {"v2/distrox", false, null, null, "stacks", null},
                {"v1/distrox", true, null, null, "stacks", null},
                {"v1/distrox/crn/test:crn:blabla", true, "test:crn:blabla", null, "stacks", null},
                {"v1/distrox/crn/test:crn:blabla/extrastuff", true, "test:crn:blabla", null, "stacks", "extrastuff"},
                {"v1/distrox/crn/test:crn:blabla/extrastuff/andmore", true, "test:crn:blabla", null, "stacks", "extrastuff/andmore"}
        });
    }
}