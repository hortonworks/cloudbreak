package com.sequenceiq.cloudbreak.structuredevent.rest.urlparsers;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class V3CredentialCodeGrantFlowParserTest {

    private static final V3CredentialCodeGrantFlowParser UNDER_TEST = new V3CredentialCodeGrantFlowParser();

    private final String example;

    private final boolean matches;

    public V3CredentialCodeGrantFlowParserTest(String example, boolean matches) {
        this.example = example;
        this.matches = matches;
    }

    @Parameterized.Parameters(name = "{index}: value: {0} matches: {1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"v3/123456789/codegrantflow/init", false},
                {"v3/11/codegrantflow/init/something", false},
                {"v3/123/-/codegrantflow/init/aotherelse", true},
                {"v3/123/a/codegrantflow/init/somesimilar", true},
                {"v3/asd/codegrantflow/init/somethingelse", false},
                {"v3/123/credentials/codegrantflow/init/tired", true},
                {"v3/123456789/codegrantflow/init/somenotsimilar", false}
        });
    }

    @Test
    public void testRestUrlPattern() {
        if (matches) {
            Assert.assertTrue(UNDER_TEST.getPattern().matcher(example).matches());
        } else {
            Assert.assertFalse(UNDER_TEST.getPattern().matcher(example).matches());
        }
    }

}