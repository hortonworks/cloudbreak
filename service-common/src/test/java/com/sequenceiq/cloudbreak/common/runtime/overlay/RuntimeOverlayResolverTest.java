package com.sequenceiq.cloudbreak.common.runtime.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.json.JsonTreeAssertions;

/**
 * Exercises {@link RuntimeOverlayResolver} against a small test-only fixture under
 * {@code common/src/test/resources/widgets/7.0.0} (frozen base) and
 * {@code common/src/test/resources/runtime-overlays/7.0.x/widgets} (overlays). It proves the reusable
 * resolution the core and datalake adapters both rely on: forward propagation, highest-anchor-wins on a
 * conflicting path, and whole-file tombstones - independent of any real template tree.
 */
class RuntimeOverlayResolverTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String BASE = "7.0.0";

    private static final String SUBTREE = "widgets";

    private static final List<String> POINTERS = List.of("/name", "/cluster/blueprintName");

    private static final Set<String> SUPPORTED = Set.of("7.0.0", "7.0.1", "7.0.2", "7.0.3");

    @Test
    void materializesOnlyTheOverlayVersionsAboveTheBase() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        assertFalse(overlays.containsKey("7.0.0"), "the base (full dir on disk) must not be materialized as an overlay");
        assertEquals(Set.of("7.0.1", "7.0.2", "7.0.3"), overlays.keySet(), "every supported version above the base is an overlay");
    }

    @Test
    void patchAppliesToTheNamedGroupAndForwardPropagates() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        // 7.0.1 patches aws/alpha's master group; 7.0.2 carries no patch of its own and must inherit it.
        assertEquals("m5.2xlarge", masterInstanceType(overlays, "7.0.1"), "7.0.1 reflects its own patch");
        assertEquals("m5.2xlarge", masterInstanceType(overlays, "7.0.2"), "the 7.0.1 change must forward-propagate into 7.0.2");
        // A sibling group in the same file is untouched by the patch.
        assertEquals("r5.large", overlays.get("7.0.1").get("aws/alpha.json").at("/instanceGroups/1/template/instanceType").asText(),
                "an unpatched group must keep the base instance type");
    }

    @Test
    void highestAnchorWinsWhenTwoVersionsPatchTheSameFile() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        // 7.0.3 re-patches the same master group on top of 7.0.1's change, so its later op wins.
        assertEquals("m5.4xlarge", masterInstanceType(overlays, "7.0.3"), "7.0.3's later-anchored patch must win over 7.0.1's");
    }

    @Test
    void tombstoneDropsAFileFromThatVersionOnward() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        assertTrue(overlays.get("7.0.2").containsKey("aws/beta.json"), "aws/beta exists in 7.0.2 (before the tombstone)");
        assertFalse(overlays.get("7.0.3").containsKey("aws/beta.json"), "aws/beta is tombstoned from 7.0.3 onward");
    }

    @Test
    void wholeFileAdditionAppearsWithVersionInjection() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        // aws/gamma has no counterpart in the 7.0.0 base; it is introduced whole at 7.0.1.
        JsonNode gamma = overlays.get("7.0.1").get("aws/gamma.json");
        assertNotNull(gamma, "a whole-file addition with no base counterpart must be materialized");
        assertEquals("7.0.1 - Gamma AWS", gamma.at("/name").asText(), "an addition's version fields are injected exactly like a base file's");
        assertEquals("7.0.1 - Gamma", gamma.at("/cluster/blueprintName").asText());
    }

    @Test
    void wholeFileAdditionForwardPropagatesToHigherVersions() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        assertTrue(overlays.get("7.0.2").containsKey("aws/gamma.json"), "an addition introduced at 7.0.1 must forward-propagate into 7.0.2");
        assertEquals("7.0.2 - Gamma AWS", overlays.get("7.0.2").get("aws/gamma.json").at("/name").asText(), "the propagated addition is injected for 7.0.2");
    }

    @Test
    void patchOnAnAddedFileApplies() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        // gamma is added at 7.0.1 and patched at 7.0.3; a patch keyed on an added path must compose with the addition.
        assertEquals("m5.xlarge", masterInstanceType(overlays, "7.0.2", "aws/gamma.json"), "before its patch the addition keeps the authored value");
        assertEquals("m5.8xlarge", masterInstanceType(overlays, "7.0.3", "aws/gamma.json"), "a patch keyed on an added file must apply");
    }

    @Test
    void tombstoneDropsAnAddedFile() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        assertTrue(overlays.get("7.0.2").containsKey("aws/delta.json"), "delta (added at 7.0.1) exists in 7.0.2");
        assertFalse(overlays.get("7.0.3").containsKey("aws/delta.json"), "an added file can itself be tombstoned at a later version");
    }

    @Test
    void versionFieldsAreInjectedForEveryOverlayVersion() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        JsonNode alpha703 = overlays.get("7.0.3").get("aws/alpha.json");
        assertEquals("7.0.3 - Alpha AWS", alpha703.at("/name").asText());
        assertEquals("7.0.3 - Alpha", alpha703.at("/cluster/blueprintName").asText());
    }

    @Test
    void unpatchedFileEqualsTheBaseModuloTheInjectedPointers() throws IOException {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);
        JsonNode base = baseWidget("azure/alpha.json");

        // azure/alpha carries no patch at any version, so it must reconstruct as pure base + version injection.
        JsonTreeAssertions.assertEqualsIgnoringPaths(base, overlays.get("7.0.2").get("azure/alpha.json"),
                Set.of("/name", "/cluster/blueprintName"),
                "an unpatched file must equal the base once the injected version pointers are excused");
    }

    @Test
    void emptySupportedSetYieldsNoOverlays() {
        assertTrue(RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, Set.of(), path -> true, POINTERS).isEmpty(),
                "an empty supported set enumerates no overlay versions");
    }

    @Test
    void resolvesABaseTreeWhoseFilesUseANonJsonSuffix() {
        // Blueprints live as .bp files; the resolver must glob the base by that suffix, match the .patch.json patch
        // to the .bp base key, and inject a bare-version leaf (cdhVersion) as well as the "<version> - " description.
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(
                "7.0.0", "gadgets", "gadgets", Set.of("7.0.0", "7.0.1"), path -> true,
                List.of("/description", "/blueprint/cdhVersion"), ".bp");

        JsonNode main = overlays.get("7.0.1").get("main.bp");
        assertNotNull(main, "the .bp base file must be discovered and keyed by its .bp relative path");
        assertEquals("m5.2xlarge", main.at("/blueprint/instanceGroups/0/template/instanceType").asText(),
                "the .patch.json patch must match the .bp base key and apply");
        assertEquals("7.0.1 - Main Gadget", main.at("/description").asText());
        assertEquals("7.0.1", main.at("/blueprint/cdhVersion").asText(), "a bare-version leaf must be rewritten whole");
    }

    private String masterInstanceType(Map<String, Map<String, JsonNode>> overlays, String version) {
        return masterInstanceType(overlays, version, "aws/alpha.json");
    }

    private String masterInstanceType(Map<String, Map<String, JsonNode>> overlays, String version, String relativePath) {
        JsonNode widget = overlays.get(version).get(relativePath);
        assertNotNull(widget, relativePath + " must be materialized for " + version);
        for (JsonNode group : widget.path("instanceGroups")) {
            if ("master".equals(group.path("name").asText())) {
                return group.at("/template/instanceType").asText();
            }
        }
        throw new IllegalStateException("master group not found in materialized " + version + " " + relativePath);
    }

    private JsonNode baseWidget(String relativePath) throws IOException {
        try (InputStream inputStream = new ClassPathResource(SUBTREE + "/" + BASE + "/" + relativePath).getInputStream()) {
            return MAPPER.readTree(inputStream);
        }
    }
}
