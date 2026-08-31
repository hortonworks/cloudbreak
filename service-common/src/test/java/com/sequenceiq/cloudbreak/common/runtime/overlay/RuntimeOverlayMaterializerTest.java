package com.sequenceiq.cloudbreak.common.runtime.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.json.JsonTreeAssertions;
import com.sequenceiq.cloudbreak.common.json.patch.JsonPatchTestFailedException;

/**
 * Unit tests for the version-agnostic {@link RuntimeOverlayMaterializer}: version injection must touch only
 * the whitelisted pointers, tombstones must drop files, and a stale {@code test} op must fail loud.
 */
class RuntimeOverlayMaterializerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<String> POINTERS = List.of("/name", "/distroXTemplate/cluster/blueprintName");

    @Test
    void versionInjectionRewritesOnlyTheWhitelistedPointers() throws JsonProcessingException {
        // The description also starts with the base version but is NOT a whitelisted pointer, so it must survive verbatim.
        JsonNode base = MAPPER.readTree("""
                {
                    "name": "7.3.3 - Data Engineering for AWS",
                    "description": "7.3.3 pinned by design",
                    "distroXTemplate": { "cluster": { "blueprintName": "7.3.3 - Data Engineering" } }
                }""");

        Map<String, JsonNode> materialized = RuntimeOverlayMaterializer.materialize(
                "7.3.3", "7.3.4", Map.of("aws/de.json", base), Map.of(), Set.of(), POINTERS);

        JsonNode result = materialized.get("aws/de.json");
        assertEquals("7.3.4 - Data Engineering for AWS", result.at("/name").asText());
        assertEquals("7.3.4 - Data Engineering", result.at("/distroXTemplate/cluster/blueprintName").asText());
        assertEquals("7.3.3 pinned by design", result.at("/description").asText(), "a non-whitelisted version-like field must not change");
    }

    @Test
    void versionInjectionRewritesOnlyThePrefixNotEveryOccurrence() throws JsonProcessingException {
        JsonNode base = MAPPER.readTree("""
                {"name": "7.3.3 - built on 7.3.3", "distroXTemplate": {"cluster": {"blueprintName": "x"}}}""");

        Map<String, JsonNode> materialized = RuntimeOverlayMaterializer.materialize(
                "7.3.3", "7.3.4", Map.of("aws/de.json", base), Map.of(), Set.of(), POINTERS);

        // Only the leading "7.3.3 " prefix is rewritten - a trailing "7.3.3" mention is left intact.
        assertEquals("7.3.4 - built on 7.3.3", materialized.get("aws/de.json").at("/name").asText());
    }

    @Test
    void versionInjectionRewritesABareBaseVersionLeaf() throws JsonProcessingException {
        // A blueprint carries the version both as a "<version> - ..." description and as a bare cdhVersion "7.3.3";
        // both whitelisted pointers must be rewritten, while a component version that merely contains the base
        // version (a parcel-like "7.3.3.0-1234") must survive verbatim.
        JsonNode base = MAPPER.readTree("""
                {
                    "description": "7.3.3 - Data Engineering Spark3",
                    "blueprint": { "cdhVersion": "7.3.3", "parcelVersion": "7.3.3.0-1234" }
                }""");

        Map<String, JsonNode> materialized = RuntimeOverlayMaterializer.materialize(
                "7.3.3", "7.3.4", Map.of("cdp-de.bp", base), Map.of(), Set.of(),
                List.of("/description", "/blueprint/cdhVersion"));

        JsonNode result = materialized.get("cdp-de.bp");
        assertEquals("7.3.4 - Data Engineering Spark3", result.at("/description").asText());
        assertEquals("7.3.4", result.at("/blueprint/cdhVersion").asText(), "a bare base-version leaf must be rewritten whole");
        assertEquals("7.3.3.0-1234", result.at("/blueprint/parcelVersion").asText(), "a non-whitelisted version-like field must not change");
    }

    @Test
    void patchIsAppliedBeforeInjection() throws JsonProcessingException {
        JsonNode base = MAPPER.readTree("""
                {"name": "7.3.3 - DE", "distroXTemplate": {"cluster": {"blueprintName": "7.3.3 - DE"},
                "instanceGroups": [{"name": "master", "template": {"instanceType": "m5.4xlarge"}}]}}""");
        JsonNode patch = MAPPER.readTree("""
                [
                {"op": "test", "path": "/distroXTemplate/instanceGroups/name=master/template/instanceType", "value": "m5.4xlarge"},
                {"op": "replace", "path": "/distroXTemplate/instanceGroups/name=master/template/instanceType", "value": "m5.8xlarge"}
                ]""");

        Map<String, JsonNode> materialized = RuntimeOverlayMaterializer.materialize(
                "7.3.3", "7.3.4", Map.of("aws/de.json", base), Map.of("aws/de.json", patch), Set.of(), POINTERS);

        JsonNode result = materialized.get("aws/de.json");
        assertEquals("m5.8xlarge", result.at("/distroXTemplate/instanceGroups/0/template/instanceType").asText());
        assertEquals("7.3.4 - DE", result.at("/name").asText(), "injection still runs after the patch");
    }

    @Test
    void tombstoneDropsTheFileFromTheMaterializedSet() throws JsonProcessingException {
        JsonNode kept = MAPPER.readTree("{\"name\": \"7.3.3 - kept\"}");
        JsonNode dropped = MAPPER.readTree("{\"name\": \"7.3.3 - dropped\"}");

        Map<String, JsonNode> materialized = RuntimeOverlayMaterializer.materialize(
                "7.3.3", "7.3.4", Map.of("aws/kept.json", kept, "aws/dropped.json", dropped), Map.of(),
                Set.of("aws/dropped.json"), POINTERS);

        assertTrue(materialized.containsKey("aws/kept.json"), "a non-tombstoned file must remain");
        assertFalse(materialized.containsKey("aws/dropped.json"), "a tombstoned file must be dropped");
    }

    @Test
    void staleTestOpFailsLoudInsteadOfCorruptingOutput() throws JsonProcessingException {
        JsonNode base = MAPPER.readTree("""
                {"name": "7.3.3 - DE", "distroXTemplate": {"cluster": {"blueprintName": "x"},
                "instanceGroups": [{"name": "master", "template": {"instanceType": "m5.4xlarge"}}]}}""");
        // The test op asserts a value that does not match the base - base drift must fail loud.
        JsonNode patch = MAPPER.readTree("""
                [
                {"op": "test", "path": "/distroXTemplate/instanceGroups/name=master/template/instanceType", "value": "WRONG"},
                {"op": "replace", "path": "/distroXTemplate/instanceGroups/name=master/template/instanceType", "value": "m5.8xlarge"}
                ]""");

        assertThrows(JsonPatchTestFailedException.class, () -> RuntimeOverlayMaterializer.materialize(
                "7.3.3", "7.3.4", Map.of("aws/de.json", base), Map.of("aws/de.json", patch), Set.of(), POINTERS));
    }

    @Test
    void unpatchedTemplateEqualsBaseModuloTheInjectedPointers() throws JsonProcessingException {
        JsonNode base = MAPPER.readTree("""
                {"name": "7.3.3 - DE", "description": "d",
                "distroXTemplate": {"cluster": {"blueprintName": "7.3.3 - DE"}, "enableLoadBalancer": false}}""");

        Map<String, JsonNode> materialized = RuntimeOverlayMaterializer.materialize(
                "7.3.3", "7.3.5", Map.of("aws/de.json", base), Map.of(), Set.of(), POINTERS);

        JsonTreeAssertions.assertEqualsIgnoringPaths(base, materialized.get("aws/de.json"),
                Set.of("/name", "/distroXTemplate/cluster/blueprintName"),
                "an unpatched template must equal the base once the injected version pointers are excused");
    }
}
