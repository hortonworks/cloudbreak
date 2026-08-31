package com.sequenceiq.cloudbreak.common.json.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Coverage for {@link JsonPatchApplier}: the {@code <field>=<value>} array-selector extension (which lets
 * overlay patches address a host group by name instead of a brittle positional index) and the four RFC 6902
 * operations the overlay model authors - a {@code replace} field change, an {@code add} that introduces a
 * new JSON section or array element, and a {@code remove} that drops one.
 */
class JsonPatchApplierTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String DOC = """
            {
                "instanceGroups": [
                    { "name": "master", "template": { "instanceType": "m5.xlarge" } },
                    { "name": "core",   "template": { "instanceType": "m5.2xlarge" } }
                ]
            }
            """;

    private static final String CONFIG_DOC = """
            {
                "instanceGroups": [
                    { "name": "master", "template": { "instanceType": "m5.xlarge" } }
                ],
                "tags": { "owner": "team-a", "env": "test" }
            }
            """;

    @Test
    void selectorResolvesArrayElementByFieldValue() throws IOException {
        JsonNode patched = JsonPatchApplier.apply(MAPPER.readTree(DOC), MAPPER.readTree("""
                [
                    { "op": "test",    "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.2xlarge" },
                    { "op": "replace", "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.4xlarge" }
                ]
                """));

        assertEquals("m5.4xlarge", patched.at("/instanceGroups/1/template/instanceType").asText());
        assertEquals("m5.xlarge", patched.at("/instanceGroups/0/template/instanceType").asText(), "the master group must be untouched");
    }

    @Test
    void selectorMatchingNoElementFailsLoud() throws IOException {
        JsonNode target = MAPPER.readTree(DOC);
        JsonNode patch = MAPPER.readTree("""
                [
                    { "op": "test",    "path": "/instanceGroups/name=idbroker/template/instanceType", "value": "m5.4xlarge" },
                    { "op": "replace", "path": "/instanceGroups/name=idbroker/template/instanceType", "value": "m5.4xlarge" }
                ]
                """);

        assertThrows(JsonPatchTestFailedException.class, () -> JsonPatchApplier.apply(target, patch));
    }

    @Test
    void staleTestValueFailsLoud() throws IOException {
        JsonNode target = MAPPER.readTree(DOC);
        // The base has m5.2xlarge; a patch authored against m5.9xlarge has drifted and must not apply silently.
        JsonNode patch = MAPPER.readTree("""
                [
                    { "op": "test",    "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.9xlarge" },
                    { "op": "replace", "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.4xlarge" }
                ]
                """);

        assertThrows(JsonPatchTestFailedException.class, () -> JsonPatchApplier.apply(target, patch));
    }

    @Test
    void applyEnforcesTheTestBeforeMutateDiscipline() throws IOException {
        JsonNode target = MAPPER.readTree(DOC);
        // A replace with no preceding test op is rejected up front by the linter, before any op is applied.
        JsonNode patch = MAPPER.readTree("""
                [
                    { "op": "replace", "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.4xlarge" }
                ]
                """);

        assertThrows(IllegalArgumentException.class, () -> JsonPatchApplier.apply(target, patch));
    }

    @Test
    void replaceChangesAScalarField() throws IOException {
        // Field change: a plain (non-selector) replace, guarded by a test asserting the current base value.
        JsonNode patched = JsonPatchApplier.apply(MAPPER.readTree(CONFIG_DOC), MAPPER.readTree("""
                [
                    { "op": "test",    "path": "/tags/owner", "value": "team-a" },
                    { "op": "replace", "path": "/tags/owner", "value": "team-b" }
                ]
                """));

        assertEquals("team-b", patched.at("/tags/owner").asText());
        assertEquals("test", patched.at("/tags/env").asText(), "a sibling field must be untouched");
    }

    @Test
    void addCreatesANewObjectSection() throws IOException {
        // Section addition: an 'add' introduces a whole new JSON section; an 'add' needs no preceding 'test' guard.
        JsonNode patched = JsonPatchApplier.apply(MAPPER.readTree(CONFIG_DOC), MAPPER.readTree("""
                [
                    { "op": "add", "path": "/services", "value": { "names": ["HDFS", "YARN"] } }
                ]
                """));

        assertEquals("HDFS", patched.at("/services/names/0").asText());
        assertEquals("YARN", patched.at("/services/names/1").asText());
        assertEquals("team-a", patched.at("/tags/owner").asText(), "an unrelated section must be untouched");
    }

    @Test
    void addAppendsAnArrayElementWithTheEndToken() throws IOException {
        JsonNode patched = JsonPatchApplier.apply(MAPPER.readTree(CONFIG_DOC), MAPPER.readTree("""
                [
                    { "op": "add", "path": "/instanceGroups/-", "value": { "name": "worker", "template": { "instanceType": "m5.2xlarge" } } }
                ]
                """));

        assertEquals(2, patched.at("/instanceGroups").size());
        assertEquals("worker", patched.at("/instanceGroups/1/name").asText(), "the '-' token appends after the last element");
    }

    @Test
    void addInsertsAnArrayElementAtAnIndex() throws IOException {
        JsonNode patched = JsonPatchApplier.apply(MAPPER.readTree(CONFIG_DOC), MAPPER.readTree("""
                [
                    { "op": "add", "path": "/instanceGroups/0", "value": { "name": "gateway" } }
                ]
                """));

        assertEquals("gateway", patched.at("/instanceGroups/0/name").asText(), "an add at an index inserts before the existing element");
        assertEquals("master", patched.at("/instanceGroups/1/name").asText(), "the previously-first element shifts right");
    }

    @Test
    void removeDropsAnObjectSection() throws IOException {
        // Section deletion: a 'remove' must be guarded by a 'test' on the same path asserting the section being dropped.
        JsonNode patched = JsonPatchApplier.apply(MAPPER.readTree(CONFIG_DOC), MAPPER.readTree("""
                [
                    { "op": "test",   "path": "/tags", "value": { "owner": "team-a", "env": "test" } },
                    { "op": "remove", "path": "/tags" }
                ]
                """));

        assertTrue(patched.at("/tags").isMissingNode(), "the whole section must be gone");
        assertEquals("master", patched.at("/instanceGroups/0/name").asText(), "an unrelated section must survive");
    }

    @Test
    void removeDropsAnArrayElementAddressedBySelector() throws IOException {
        // The guarding test and the remove must carry the identical path (compared textually), so the selector
        // appears on both; the applier resolves it to the matching index for each.
        JsonNode patched = JsonPatchApplier.apply(MAPPER.readTree(DOC), MAPPER.readTree("""
                [
                    { "op": "test",   "path": "/instanceGroups/name=master", "value": { "name": "master", "template": { "instanceType": "m5.xlarge" } } },
                    { "op": "remove", "path": "/instanceGroups/name=master" }
                ]
                """));

        assertEquals(1, patched.at("/instanceGroups").size(), "the master element must be removed");
        assertEquals("core", patched.at("/instanceGroups/0/name").asText(), "the surviving element is the former second group");
    }
}
