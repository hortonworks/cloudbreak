package com.sequenceiq.cloudbreak.common.json.patch;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Coverage for {@link JsonPatchLinter}: the {@code test}-before-mutate discipline that keeps cumulative
 * overlay chains safe. Every {@code replace}/{@code remove} must be immediately preceded by a
 * {@code test} op on the same path; {@code add} and {@code test} need no guard.
 */
class JsonPatchLinterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void guardedReplaceIsAccepted() throws IOException {
        assertDoesNotThrow(() -> JsonPatchLinter.validate(patch("""
                [
                    { "op": "test",    "path": "/a/b", "value": 1 },
                    { "op": "replace", "path": "/a/b", "value": 2 }
                ]
                """)));
    }

    @Test
    void guardedRemoveIsAccepted() throws IOException {
        assertDoesNotThrow(() -> JsonPatchLinter.validate(patch("""
                [
                    { "op": "test",   "path": "/a/b", "value": 1 },
                    { "op": "remove", "path": "/a/b" }
                ]
                """)));
    }

    @Test
    void bareAddNeedsNoGuard() throws IOException {
        assertDoesNotThrow(() -> JsonPatchLinter.validate(patch("""
                [
                    { "op": "add", "path": "/a/c", "value": 3 }
                ]
                """)));
    }

    @Test
    void selectorPathsAreComparedTextually() throws IOException {
        assertDoesNotThrow(() -> JsonPatchLinter.validate(patch("""
                [
                    { "op": "test",    "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.2xlarge" },
                    { "op": "replace", "path": "/instanceGroups/name=core/template/instanceType", "value": "m5.4xlarge" }
                ]
                """)));
    }

    @Test
    void replaceWithoutAPrecedingTestIsRejected() throws IOException {
        JsonNode patch = patch("""
                [
                    { "op": "replace", "path": "/a/b", "value": 2 }
                ]
                """);
        assertThrows(IllegalArgumentException.class, () -> JsonPatchLinter.validate(patch));
    }

    @Test
    void removeWithoutAPrecedingTestIsRejected() throws IOException {
        JsonNode patch = patch("""
                [
                    { "op": "remove", "path": "/a/b" }
                ]
                """);
        assertThrows(IllegalArgumentException.class, () -> JsonPatchLinter.validate(patch));
    }

    @Test
    void testOnADifferentPathDoesNotGuardTheReplace() throws IOException {
        JsonNode patch = patch("""
                [
                    { "op": "test",    "path": "/a/x", "value": 1 },
                    { "op": "replace", "path": "/a/b", "value": 2 }
                ]
                """);
        assertThrows(IllegalArgumentException.class, () -> JsonPatchLinter.validate(patch));
    }

    @Test
    void aTestGuardsOnlyTheImmediatelyFollowingMutation() throws IOException {
        // The first replace consumes the test; the second replace is then unguarded.
        JsonNode patch = patch("""
                [
                    { "op": "test",    "path": "/a/b", "value": 1 },
                    { "op": "replace", "path": "/a/b", "value": 2 },
                    { "op": "replace", "path": "/a/b", "value": 3 }
                ]
                """);
        assertThrows(IllegalArgumentException.class, () -> JsonPatchLinter.validate(patch));
    }

    @Test
    void nonArrayInputIsRejected() throws IOException {
        JsonNode notAPatch = patch("""
                { "op": "replace", "path": "/a/b", "value": 2 }
                """);
        assertThrows(IllegalArgumentException.class, () -> JsonPatchLinter.validate(notAPatch));
    }

    private JsonNode patch(String json) throws IOException {
        return MAPPER.readTree(json);
    }
}
