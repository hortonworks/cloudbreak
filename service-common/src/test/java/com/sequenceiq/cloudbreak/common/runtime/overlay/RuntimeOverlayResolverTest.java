package com.sequenceiq.cloudbreak.common.runtime.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
 * conflicting path, whole-file tombstones, additions and replacements - independent of any real template tree.
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

        // beta is a plain base file with no whole-file delta of its own, so this covers tombstoning the base.
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
    void wholeFileReplacementSupersedesTheBaseFile() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        // aws/epsilon exists in the 7.0.0 base and is rewritten wholesale at 7.0.2.
        JsonNode epsilon = overlays.get("7.0.2").get("aws/epsilon.json");
        assertNotNull(epsilon, "a replaced base file must still be materialized");
        assertEquals("rewritten", epsilon.path("shape").asText(), "the replacement's body must supersede the base body");
        assertEquals(1, epsilon.path("instanceGroups").size(), "the replacement drops the base's second group, so nothing is merged in from the base");
        assertEquals("7.0.2 - Epsilon AWS", epsilon.at("/name").asText(), "a replacement's version fields are injected exactly like a base file's");
        assertEquals("7.0.2 - Epsilon", epsilon.at("/cluster/blueprintName").asText());
    }

    @Test
    void wholeFileReplacementForwardPropagatesToHigherVersions() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        JsonNode epsilon = overlays.get("7.0.3").get("aws/epsilon.json");
        assertEquals("rewritten", epsilon.path("shape").asText(), "a replacement anchored at 7.0.2 must forward-propagate into 7.0.3");
        assertEquals("7.0.3 - Epsilon AWS", epsilon.at("/name").asText(), "the propagated replacement is injected for 7.0.3");
    }

    @Test
    void versionBelowTheReplacementAnchorKeepsTheBaseBody() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        // 7.0.1 is below the replacement's 7.0.2 anchor, so it still sees the base file (patched by its own overlay).
        assertEquals("legacy", overlays.get("7.0.1").get("aws/epsilon.json").path("shape").asText(),
                "a version below the replacement's anchor must keep the base body");
    }

    @Test
    void replacementFileIsNotAlsoMaterializedAsAnAddition() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        // A .replace.json also ends in .json, so the addition glob sees it too; it must be filtered out there, otherwise
        // it materializes a second, bogus template under its own .replace path - which clashes with no base path, so
        // neither fail-loud guard catches it.
        assertFalse(overlays.get("7.0.2").containsKey("aws/epsilon.replace.json"), "a replacement must not double as a whole-file addition");
        assertTrue(overlays.values().stream().flatMap(byPath -> byPath.keySet().stream()).noneMatch(path -> path.contains(".replace")),
                "no materialized path may carry the .replace marker, got: " + overlays);
    }

    @Test
    void sameAnchorPatchAppliesOnTopOfTheReplacement() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        // theta is replaced and patched at the same 7.0.2 anchor: the reset must drop only what LOWER anchors
        // accumulated, so the overlay's own patch still runs - and it tests for the replacement's value, so it only
        // succeeds against the replaced body.
        assertEquals("c6.12xlarge", masterInstanceType(overlays, "7.0.2", "aws/theta.json"),
                "a patch anchored at the replacement's own version must apply on top of the replaced body");
        assertEquals("c6.12xlarge", masterInstanceType(overlays, "7.0.3", "aws/theta.json"), "the same-anchor patch forward-propagates with the replacement");
    }

    @Test
    void patchAnchoredBelowAnAdditionIsDroppedWhenTheFileIsReAdded() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        // eta is added at 7.0.1, patched at 7.0.2, then re-added whole at 7.0.3 (additions are last-anchor-wins). The
        // 7.0.2 patch was written against the first body, so from 7.0.3 it must be dropped, not replayed.
        assertEquals("patched", overlays.get("7.0.2").get("aws/eta.json").path("tier").asText(), "the 7.0.2 patch applies to the body added at 7.0.1");
        assertEquals("readded", overlays.get("7.0.3").get("aws/eta.json").path("shape").asText(), "the later addition supersedes the earlier one");
        assertFalse(overlays.get("7.0.3").get("aws/eta.json").has("tier"),
                "a patch anchored below the re-addition must be discarded, leaving the newly added body untouched");
    }

    @Test
    void wholeFileDeltaAnchoredAboveATombstoneRevivesTheFile() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        // iota is a base file tombstoned at 7.0.1 and replaced whole at 7.0.2; kappa is added at 7.0.1, tombstoned at
        // 7.0.2 and added again at 7.0.3. A whole-file delta resets the file, tombstones from lower anchors included.
        assertFalse(overlays.get("7.0.1").containsKey("aws/iota.json"), "iota is tombstoned at 7.0.1");
        assertEquals("revived", overlays.get("7.0.2").get("aws/iota.json").path("shape").asText(),
                "a replacement anchored above the tombstone must bring the file back with its new body");

        assertFalse(overlays.get("7.0.2").containsKey("aws/kappa.json"), "kappa (added at 7.0.1) is tombstoned at 7.0.2");
        assertEquals("revived", overlays.get("7.0.3").get("aws/kappa.json").path("shape").asText(),
                "an addition anchored above the tombstone must bring the file back with its new body");
        assertEquals("7.0.3 - Kappa AWS", overlays.get("7.0.3").get("aws/kappa.json").at("/name").asText(), "the revived file is injected like any other");
    }

    @Test
    void patchAnchoredBelowAReplacementIsDroppedFromThatVersionOnward() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        // The 7.0.1 patch was written against the base body; from 7.0.2 the file is replaced, so replaying it would
        // run its guarded test ops against content it was never written for. It must be dropped, not replayed.
        assertEquals("m5.2xlarge", masterInstanceType(overlays, "7.0.1", "aws/epsilon.json"), "the 7.0.1 patch applies to the base body at its own anchor");
        assertEquals("c6.4xlarge", masterInstanceType(overlays, "7.0.2", "aws/epsilon.json"),
                "a patch anchored below the replacement must be discarded, leaving the replacement's own value");
    }

    @Test
    void patchAnchoredAboveAReplacementAppliesOnTopOfIt() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        // The 7.0.3 patch tests for the replacement's value, so it only applies if it runs against the replaced body.
        assertEquals("c6.8xlarge", masterInstanceType(overlays, "7.0.3", "aws/epsilon.json"),
                "a patch anchored above the replacement must apply on top of the replaced body");
    }

    @Test
    void tombstoneDropsAReplacedFile() {
        Map<String, Map<String, JsonNode>> overlays = RuntimeOverlayResolver.resolveOverlays(BASE, SUBTREE, SUBTREE, SUPPORTED, path -> true, POINTERS);

        assertEquals("rewritten", overlays.get("7.0.2").get("aws/zeta.json").path("shape").asText(), "zeta is replaced whole at 7.0.1");
        assertFalse(overlays.get("7.0.3").containsKey("aws/zeta.json"), "a replaced file can itself be tombstoned at a later version");
    }

    @Test
    void failsLoudWhenAReplacementHasNoBaseCounterpart() {
        // orphans/7.0.0 ships aws/present.json only, while the 7.0.1 overlay replaces aws/missing.json: the intent was
        // an addition, not a replacement, so the resolver must not silently introduce the file.
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> RuntimeOverlayResolver.resolveOverlays(BASE, "orphans", "orphans", SUPPORTED, path -> true, POINTERS));
        assertTrue(thrown.getMessage().contains("aws/missing.json"), "the failure must name the dangling replacement, got: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains("addition"), "the failure must point at the right verb, got: " + thrown.getMessage());
    }

    @Test
    void failsLoudWhenAnAdditionClashesWithABaseFileAndPointsAtTheReplacementFlavor() {
        // clashes/7.0.0 already ships aws/present.json, so the 7.0.1 whole-file addition of the same path is an
        // authoring error: the intent was either a patch or an explicit .replace override.
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> RuntimeOverlayResolver.resolveOverlays(BASE, "clashes", "clashes", SUPPORTED, path -> true, POINTERS));
        assertTrue(thrown.getMessage().contains("aws/present.json"), "the failure must name the clashing addition, got: " + thrown.getMessage());
        assertTrue(thrown.getMessage().contains(".patch.json") && thrown.getMessage().contains(".replace"),
                "the failure must point at both alternative verbs, got: " + thrown.getMessage());
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
    void failsLoudWhenTheConfiguredBaseHasNoOnDiskTemplates() {
        // Overlays are requested but the configured base version has no on-disk directory: a mis-set base must
        // fail loud at resolution time rather than silently disabling every overlay.
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> RuntimeOverlayResolver.resolveOverlays("6.0.0", SUBTREE, SUBTREE, Set.of("6.0.1"), path -> true, POINTERS));
        assertTrue(thrown.getMessage().contains("6.0.0"), "the failure must name the mis-configured base version, got: " + thrown.getMessage());
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
