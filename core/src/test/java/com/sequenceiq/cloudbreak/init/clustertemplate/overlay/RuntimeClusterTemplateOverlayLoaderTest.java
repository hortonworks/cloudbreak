package com.sequenceiq.cloudbreak.init.clustertemplate.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.json.JsonTreeAssertions;
import com.sequenceiq.cloudbreak.common.runtime.overlay.RuntimeOverlayConstants;

/**
 * Exercises {@link RuntimeClusterTemplateOverlayLoader} against the real overlay resources committed under
 * {@code core/src/main/resources/runtime-overlays/} and the frozen base cluster templates on disk. It proves
 * the base+overlay path for the DB-synced-by-name cluster-template tree:
 * <ul>
 *   <li>7.3.4 and 7.3.5 are materialized entirely from the 7.3.3 base - no full template dir on disk and
 *       (as shipped) no overlay patch of their own, so they equal the base modulo version injection,</li>
 *   <li>every synthesized {@code /name} and {@code blueprintName} is the base value with only the version
 *       prefix swapped (the guardrail for the DB-name-stable tree),</li>
 *   <li>a name-addressed instance-type patch (the test-only 7.3.6 fixture) changes exactly one field and
 *       forward-propagates into the empty 7.3.7 overlay above it, and</li>
 *   <li>a later version can tombstone a whole template or add a brand-new one.</li>
 * </ul>
 * <p>Chain mechanics that need two patch anchors (highest-anchor-wins on a shared path) are proven generically
 * for the shared resolver in {@code RuntimeOverlayResolverTest}; this test focuses on cluster-template wiring.
 */
class RuntimeClusterTemplateOverlayLoaderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String BASE_VERSION = RuntimeOverlayConstants.BASE_VERSION;

    private static final String NAME_POINTER = "/name";

    private static final String BLUEPRINT_NAME_POINTER = "/distroXTemplate/cluster/blueprintName";

    private static final String MASTER_INSTANCE_TYPE_POINTER = "/distroXTemplate/instanceGroups/0/template/instanceType";

    private static final Set<String> PATCHED = Set.of("7.3.4", "7.3.5");

    // Adds 7.3.6, which exists only as a test-only overlay under src/test/resources/runtime-overlays/7.3.6:
    // it patches aws/dataengineering-spark3's master group, tombstones aws/hybrid-spark3, and adds aws/streaming-analytics.
    private static final Set<String> PATCHED_WITH_736 = Set.of("7.3.4", "7.3.5", "7.3.6");

    // Adds an empty 7.3.7 overlay (no dir at all) above 7.3.6 to prove the 7.3.6 patch forward-propagates.
    private static final Set<String> PATCHED_WITH_737 = Set.of("7.3.4", "7.3.5", "7.3.6", "7.3.7");

    private static final String DATAENGINEERING_SPARK3 = "aws/dataengineering-spark3.json";

    private final RuntimeClusterTemplateOverlayLoader underTest = new RuntimeClusterTemplateOverlayLoader();

    @Test
    void materializesTheOverlayVersionsWithVersionFieldsInjected() throws IOException {
        Map<String, String> materialized = underTest.materializeOverlayClusterTemplates(PATCHED);

        assertFalse(materialized.isEmpty(), "expected overlay cluster templates to be materialized");
        for (Map.Entry<String, String> entry : materialized.entrySet()) {
            JsonNode template = MAPPER.readTree(entry.getValue());
            String name = template.at(NAME_POINTER).asText();
            String blueprintName = template.at(BLUEPRINT_NAME_POINTER).asText();
            assertTrue(name.startsWith("7.3.4 ") || name.startsWith("7.3.5 "), "every /name must carry an overlay version prefix, got: " + name);
            assertEquals(name, entry.getKey(), "the map key must be the template /name");
            assertTrue(blueprintName.startsWith("7.3.4 ") || blueprintName.startsWith("7.3.5 "),
                    "every blueprintName must carry an overlay version prefix, got: " + blueprintName);
        }
    }

    @Test
    void everySynthesizedNameIsTheBaseNameWithOnlyTheVersionPrefixSwapped() throws IOException {
        Map<String, JsonNode> baseByRelativePath = loadBaseTemplates();
        Map<String, String> materialized734 = underTest.materializeOverlayClusterTemplates(Set.of("7.3.4"));

        // Name stability is the guardrail for the DB-synced-by-name tree: the materialized name/blueprintName set must be
        // exactly the base set with 7.3.3 -> 7.3.4 at the prefix, byte-for-byte.
        Set<String> expectedNames = baseByRelativePath.values().stream()
                .map(base -> swapPrefix(base.at(NAME_POINTER).asText(), "7.3.4"))
                .collect(Collectors.toSet());
        assertEquals(expectedNames, materialized734.keySet(), "the 7.3.4 name set must be the base names with only the prefix swapped");

        Set<String> expectedBlueprintNames = baseByRelativePath.values().stream()
                .map(base -> swapPrefix(base.at(BLUEPRINT_NAME_POINTER).asText(), "7.3.4"))
                .collect(Collectors.toSet());
        Set<String> actualBlueprintNames = materialized734.values().stream()
                .map(this::readBlueprintName)
                .collect(Collectors.toSet());
        assertEquals(expectedBlueprintNames, actualBlueprintNames, "the 7.3.4 blueprintName set must be the base names with only the prefix swapped");
    }

    @Test
    void patchedTemplateDiffersFromTheBaseOnlyAtTheVersionFieldsAndTheTargetedInstanceType() throws IOException {
        Map<String, String> materialized = underTest.materializeOverlayClusterTemplates(PATCHED_WITH_736);
        JsonNode base = baseTemplate(DATAENGINEERING_SPARK3);
        JsonNode materialized736 = MAPPER.readTree(materialized.get(swapPrefix(base.at(NAME_POINTER).asText(), "7.3.6")));

        List<String> differences = JsonTreeAssertions.differingPaths(base, materialized736);

        assertEquals(3, differences.size(), "exactly the two version fields and one instance type should differ, got: " + differences);
        assertTrue(differences.contains(NAME_POINTER), "the injected /name must differ, got: " + differences);
        assertTrue(differences.contains(BLUEPRINT_NAME_POINTER), "the injected blueprintName must differ, got: " + differences);
        assertTrue(differences.stream().anyMatch(path -> path.endsWith("/template/instanceType")),
                "the one patched instance type must differ, got: " + differences);
    }

    @Test
    void unpatchedTemplateEqualsTheBaseModuloTheInjectedVersionFields() throws IOException {
        Map<String, String> materialized = underTest.materializeOverlayClusterTemplates(PATCHED);
        JsonNode base = baseTemplate("aws/datamart.json");
        JsonNode materialized734 = MAPPER.readTree(materialized.get(swapPrefix(base.at(NAME_POINTER).asText(), "7.3.4")));

        JsonTreeAssertions.assertEqualsIgnoringPaths(base, materialized734, Set.of(NAME_POINTER, BLUEPRINT_NAME_POINTER),
                "an unpatched cluster template must equal the 7.3.3 base once the injected version fields are excused");
    }

    @Test
    void instanceTypePatchAppliesAndForwardPropagates() throws IOException {
        Map<String, String> materialized = underTest.materializeOverlayClusterTemplates(PATCHED_WITH_737);

        // 7.3.4 and 7.3.5 carry no overlay patch of their own, so the master group keeps the 7.3.3 base value.
        assertEquals("m5.4xlarge", masterInstanceType(materialized, "7.3.4"), "7.3.4 is an unpatched overlay: master keeps the base instance type");
        assertEquals("m5.4xlarge", masterInstanceType(materialized, "7.3.5"), "7.3.5 is an unpatched overlay: master keeps the base instance type");
        // The 7.3.6 fixture patches the master group, and the change forward-propagates into the empty 7.3.7 overlay.
        assertEquals("m5.16xlarge", masterInstanceType(materialized, "7.3.6"), "7.3.6's patch applies to the master group");
        assertEquals("m5.16xlarge", masterInstanceType(materialized, "7.3.7"), "the 7.3.6 change must forward-propagate into 7.3.7");
    }

    @Test
    void tombstoneDropsATemplateFromThatVersionOnward() {
        Map<String, String> materialized = underTest.materializeOverlayClusterTemplates(PATCHED_WITH_736);

        assertNotNull(materialized.get("7.3.5 - Hybrid Spark3 for AWS"), "hybrid-spark3 exists in 7.3.5 (before the tombstone)");
        assertFalse(materialized.containsKey("7.3.6 - Hybrid Spark3 for AWS"), "hybrid-spark3 is tombstoned from 7.3.6 onward");
    }

    @Test
    void newTemplateAddedByAnOverlayIsMaterializedUnderItsInjectedName() throws IOException {
        Map<String, String> materialized = underTest.materializeOverlayClusterTemplates(PATCHED_WITH_736);

        // aws/streaming-analytics has no 7.3.3 base counterpart; it is added whole at 7.3.6 and keyed by its injected /name.
        String raw = materialized.get("7.3.6 - Streaming Analytics for AWS");
        assertNotNull(raw, "a brand-new cluster template added by an overlay must be materialized under its injected name");
        assertEquals("7.3.6 - Streaming Analytics", MAPPER.readTree(raw).at(BLUEPRINT_NAME_POINTER).asText(),
                "the addition's blueprintName must be version-injected too");
        assertFalse(materialized.containsKey("7.3.4 - Streaming Analytics for AWS"), "an addition anchored at 7.3.6 must not appear in earlier versions");
    }

    private String masterInstanceType(Map<String, String> materialized, String version) throws IOException {
        String name = version + " - Data Engineering Spark3 for AWS";
        String raw = materialized.get(name);
        assertNotNull(raw, "aws dataengineering-spark3 must be materialized for " + version);
        return MAPPER.readTree(raw).at(MASTER_INSTANCE_TYPE_POINTER).asText();
    }

    private String swapPrefix(String baseValue, String version) {
        return version + baseValue.substring(BASE_VERSION.length());
    }

    private String readBlueprintName(String rawJson) {
        try {
            return MAPPER.readTree(rawJson).at(BLUEPRINT_NAME_POINTER).asText();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private JsonNode baseTemplate(String relativePath) throws IOException {
        try (InputStream inputStream = new PathMatchingResourcePatternResolver()
                .getResource("classpath:defaults/clustertemplates/" + BASE_VERSION + "/" + relativePath).getInputStream()) {
            return MAPPER.readTree(inputStream);
        }
    }

    private Map<String, JsonNode> loadBaseTemplates() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:defaults/clustertemplates/" + BASE_VERSION + "/**/*.json");
        String marker = "/defaults/clustertemplates/" + BASE_VERSION + "/";
        Map<String, JsonNode> byRelativePath = new LinkedHashMap<>();
        for (Resource resource : resources) {
            String fullPath = resource.getURL().getPath();
            int index = fullPath.indexOf(marker);
            if (index >= 0) {
                String relativePath = fullPath.substring(index + marker.length());
                try (InputStream inputStream = resource.getInputStream()) {
                    byRelativePath.put(relativePath, MAPPER.readTree(inputStream));
                }
            }
        }
        return byRelativePath;
    }
}
