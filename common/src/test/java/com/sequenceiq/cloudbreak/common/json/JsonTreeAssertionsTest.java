package com.sequenceiq.cloudbreak.common.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonTreeAssertionsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void differingPathsReturnsEmptyForSemanticallyEqualTrees() throws IOException {
        JsonNode a = read("{ \"b\": 1, \"a\": 2 }");
        JsonNode b = read("{ \"a\": 2, \"b\": 1 }");

        assertEquals(List.of(), JsonTreeAssertions.differingPaths(a, b), "field order must not count as a difference");
    }

    @Test
    void differingPathsReportsAChangedLeafValue() throws IOException {
        JsonNode expected = read("{ \"cluster\": { \"blueprintName\": \"7.3.3 - x\" } }");
        JsonNode actual = read("{ \"cluster\": { \"blueprintName\": \"7.3.4 - x\" } }");

        assertEquals(List.of("/cluster/blueprintName"), JsonTreeAssertions.differingPaths(expected, actual));
    }

    @Test
    void differingPathsReportsPresenceDifferencesOnBothSides() throws IOException {
        JsonNode expected = read("{ \"a\": 1, \"only\": \"expected\" }");
        JsonNode actual = read("{ \"a\": 1, \"extra\": \"actual\" }");

        assertEquals(List.of("/extra", "/only"), JsonTreeAssertions.differingPaths(expected, actual).stream().sorted().toList());
    }

    @Test
    void differingPathsWalksArraysByIndex() throws IOException {
        JsonNode expected = read("{ \"instanceGroups\": [ { \"instanceType\": \"m5.xlarge\" }, { \"instanceType\": \"m5.2xlarge\" } ] }");
        JsonNode actual = read("{ \"instanceGroups\": [ { \"instanceType\": \"m5.xlarge\" }, { \"instanceType\": \"m5.4xlarge\" } ] }");

        assertEquals(List.of("/instanceGroups/1/instanceType"), JsonTreeAssertions.differingPaths(expected, actual));
    }

    @Test
    void differingPathsEscapesSlashInFieldNames() throws IOException {
        JsonNode expected = read("{ \"a/b\": 1 }");
        JsonNode actual = read("{ \"a/b\": 2 }");

        assertEquals(List.of("/a~1b"), JsonTreeAssertions.differingPaths(expected, actual), "'/' in a field name must be RFC 6901 escaped as ~1");
    }

    @Test
    void assertSemanticallyEqualsPassesForEqualTrees() throws IOException {
        JsonTreeAssertions.assertSemanticallyEquals(read("{ \"a\": [1, 2] }"), read("{ \"a\": [1, 2] }"), "should be equal");
    }

    @Test
    void assertSemanticallyEqualsFailsWithReadablePathAndValues() throws IOException {
        JsonNode expected = read("{ \"cluster\": { \"blueprintName\": \"7.3.3 - x\" } }");
        JsonNode actual = read("{ \"cluster\": { \"blueprintName\": \"7.3.4 - x\" } }");

        AssertionError error = assertThrows(AssertionError.class,
                () -> JsonTreeAssertions.assertSemanticallyEquals(expected, actual, "trees diverged"));

        assertTrue(error.getMessage().contains("trees diverged"), "keeps the caller message");
        assertTrue(error.getMessage().contains("/cluster/blueprintName"), "names the differing path");
        assertTrue(error.getMessage().contains("7.3.3") && error.getMessage().contains("7.3.4"), "shows expected and actual values");
    }

    @Test
    void assertEqualsIgnoringPathsPassesWhenOnlyIgnoredFieldsDiffer() throws IOException {
        JsonNode expected = read("{ \"cluster\": { \"blueprintName\": \"7.3.3 - x\" }, \"instanceType\": \"m5.xlarge\" }");
        JsonNode actual = read("{ \"cluster\": { \"blueprintName\": \"7.3.4 - x\" }, \"instanceType\": \"m5.xlarge\" }");

        JsonTreeAssertions.assertEqualsIgnoringPaths(expected, actual, Set.of("/cluster/blueprintName"),
                "identical apart from the injected version field");
    }

    @Test
    void assertEqualsIgnoringPathsStillFailsOnAnUnignoredDifference() throws IOException {
        JsonNode expected = read("{ \"cluster\": { \"blueprintName\": \"7.3.3 - x\" }, \"instanceType\": \"m5.xlarge\" }");
        JsonNode actual = read("{ \"cluster\": { \"blueprintName\": \"7.3.4 - x\" }, \"instanceType\": \"m5.4xlarge\" }");

        AssertionError error = assertThrows(AssertionError.class,
                () -> JsonTreeAssertions.assertEqualsIgnoringPaths(expected, actual, Set.of("/cluster/blueprintName"), "should still catch the real drift"));

        assertTrue(error.getMessage().contains("/instanceType"), "the un-ignored path must still be reported");
    }

    @Test
    void assertEqualsIgnoringPathsSkipsAbsentPathsGracefully() throws IOException {
        JsonNode expected = read("{ \"a\": 1 }");
        JsonNode actual = read("{ \"a\": 1 }");

        JsonTreeAssertions.assertEqualsIgnoringPaths(expected, actual, Set.of("/not/present"), "absent ignore path is a no-op");
    }

    private static JsonNode read(String json) throws IOException {
        return MAPPER.readTree(json);
    }
}
