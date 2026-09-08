package com.sequenceiq.cloudbreak.init.blueprint.overlay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.common.json.JsonTreeAssertions;
import com.sequenceiq.cloudbreak.common.runtime.overlay.RuntimeOverlayConstants;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintEntities;
import com.sequenceiq.cloudbreak.init.blueprint.overlay.RuntimeBlueprintOverlayLoader.MaterializedBlueprint;

/**
 * Exercises {@link RuntimeBlueprintOverlayLoader} against the real 7.3.3 base {@code .bp} files and the overlay
 * resources committed under {@code core/src/main/resources/runtime-overlays/}, plus a test-only 7.3.6 overlay
 * under {@code core/src/test/resources/runtime-overlays/7.3.6}. Blueprints are the DB-synced-by-name tree whose
 * display name is external to the file (it is the left-hand side of the {@code displayName=fileStem} entry in
 * the {@code cb.blueprint.cm.defaults.<version>} block), so this proves:
 * <ul>
 *   <li>every synthesized name is the base name with only the {@code 7.3.3 -> <version>} prefix swapped,</li>
 *   <li>the in-file version fields ({@code /description} and the bare {@code /blueprint/cdhVersion}) are injected,</li>
 *   <li>7.3.4 and 7.3.5 ship no overlay patch, so they equal the base modulo those injected version fields,</li>
 *   <li>a config patch (the test-only 7.3.6 fixture) changes exactly one field and forward-propagates into the
 *       empty 7.3.7 overlay above it, tombstones a whole blueprint, and adds a brand-new one, and</li>
 *   <li>only stems registered in the base block become overlays.</li>
 * </ul>
 * <p>Chain mechanics that need two patch anchors (highest-anchor-wins on a shared path) are proven generically
 * for the shared resolver in {@code RuntimeOverlayResolverTest}; this test focuses on blueprint-specific wiring.
 */
class RuntimeBlueprintOverlayLoaderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String BASE_VERSION = RuntimeOverlayConstants.BASE_VERSION;

    private static final String DESCRIPTION_POINTER = "/description";

    private static final String CDH_VERSION_POINTER = "/blueprint/cdhVersion";

    private static final Set<String> PATCHED = Set.of("7.3.4", "7.3.5");

    // Adds 7.3.6, which exists only as a test-only overlay under src/test/resources/runtime-overlays/7.3.6:
    // it patches cdp-data-engineering-spark3's fs_trash_interval, tombstones cdp-data-mart, and adds cdp-brand-new.
    private static final Set<String> PATCHED_WITH_736 = Set.of("7.3.4", "7.3.5", "7.3.6");

    // Adds an empty 7.3.7 overlay (no dir at all) above 7.3.6 to prove the 7.3.6 patch forward-propagates.
    private static final Set<String> PATCHED_WITH_737 = Set.of("7.3.4", "7.3.5", "7.3.6", "7.3.7");

    // Real base display names (the left-hand side of the cb.blueprint.cm.defaults.7.3.3 block entries).
    private static final String DE_SPARK3_STEM = "cdp-data-engineering-spark3";

    private static final String DE_SPARK3_NAME = "7.3.3 - Data Engineering: Apache Spark3, Apache Hive, Apache Oozie";

    private static final String DATA_MART_STEM = "cdp-data-mart";

    private static final String DATA_MART_NAME = "7.3.3 - Data Mart: Apache Impala, Hue";

    private static final String SDX_STEM = "cdp-sdx";

    private static final String SDX_NAME = "7.3.3 - SDX Light Duty: Apache Hive Metastore, Apache Ranger, Apache Atlas";

    private final RuntimeBlueprintOverlayLoader underTest = new RuntimeBlueprintOverlayLoader();

    @BeforeEach
    void setUp() {
        BlueprintEntities entities = new BlueprintEntities();
        // Only three stems are registered here; the resolver globs all 34 base .bp files, so this also proves that
        // an unregistered stem (every other .bp on disk) is deliberately not surfaced as an overlay.
        entities.getDefaults().put(BASE_VERSION, String.join(";",
                DE_SPARK3_NAME + "=" + DE_SPARK3_STEM,
                DATA_MART_NAME + "=" + DATA_MART_STEM,
                SDX_NAME + "=" + SDX_STEM));
        ReflectionTestUtils.setField(underTest, "blueprintEntities", entities);
    }

    @Test
    void materializesTheOverlayVersionsWithInFileVersionFieldsInjected() {
        Map<String, MaterializedBlueprint> byName = byName(underTest.materializeOverlayBlueprints(PATCHED));

        assertFalse(byName.isEmpty(), "expected overlay blueprints to be materialized");
        for (MaterializedBlueprint materialized : byName.values()) {
            String name = materialized.name();
            String version = name.substring(0, BASE_VERSION.length());
            assertTrue(name.startsWith("7.3.4 ") || name.startsWith("7.3.5 "), "every name must carry an overlay version prefix, got: " + name);
            assertTrue(materialized.fileJson().at(DESCRIPTION_POINTER).asText().startsWith(version + " "),
                    "the /description prefix must carry the overlay version, got: " + materialized.fileJson().at(DESCRIPTION_POINTER).asText());
            assertEquals(version, materialized.fileJson().at(CDH_VERSION_POINTER).asText(),
                    "the bare /blueprint/cdhVersion leaf must be rewritten whole to the overlay version");
        }
        // Only the three registered stems become overlays, across both versions.
        assertEquals(6, byName.size(), "exactly the three registered stems must be materialized for 7.3.4 and 7.3.5");
    }

    @Test
    void everySynthesizedNameIsTheBaseNameWithOnlyTheVersionPrefixSwapped() {
        Map<String, MaterializedBlueprint> byName = byName(underTest.materializeOverlayBlueprints(Set.of("7.3.4")));

        Set<String> expectedNames = Set.of(
                swapPrefix(DE_SPARK3_NAME, "7.3.4"),
                swapPrefix(DATA_MART_NAME, "7.3.4"),
                swapPrefix(SDX_NAME, "7.3.4"));
        assertEquals(expectedNames, byName.keySet(), "the 7.3.4 name set must be the base names with only the prefix swapped");
    }

    @Test
    void patchedBlueprintDiffersFromTheBaseOnlyAtTheVersionFieldsAndTheTargetedConfig() throws IOException {
        Map<String, MaterializedBlueprint> byName = byName(underTest.materializeOverlayBlueprints(PATCHED_WITH_736));
        JsonNode base = baseBlueprint(DE_SPARK3_STEM);
        JsonNode materialized736 = byName.get(swapPrefix(DE_SPARK3_NAME, "7.3.6")).fileJson();

        List<String> differences = JsonTreeAssertions.differingPaths(base, materialized736);

        assertEquals(3, differences.size(), "exactly the two version fields and the one patched config should differ, got: " + differences);
        assertTrue(differences.contains(DESCRIPTION_POINTER), "the injected /description must differ, got: " + differences);
        assertTrue(differences.contains(CDH_VERSION_POINTER), "the injected /blueprint/cdhVersion must differ, got: " + differences);
        assertTrue(differences.stream().anyMatch(path -> path.endsWith("/fs_trash_interval/value") || path.endsWith("/value")),
                "the one patched config value must differ, got: " + differences);
    }

    @Test
    void unpatchedBlueprintEqualsTheBaseModuloTheInjectedVersionFields() throws IOException {
        Map<String, MaterializedBlueprint> byName = byName(underTest.materializeOverlayBlueprints(PATCHED));
        JsonNode base = baseBlueprint(SDX_STEM);
        JsonNode materialized734 = byName.get(swapPrefix(SDX_NAME, "7.3.4")).fileJson();

        JsonTreeAssertions.assertEqualsIgnoringPaths(base, materialized734, Set.of(DESCRIPTION_POINTER, CDH_VERSION_POINTER),
                "an unpatched blueprint must equal the 7.3.3 base once the injected version fields are excused");
    }

    @Test
    void configPatchAppliesAndForwardPropagates() {
        Map<String, MaterializedBlueprint> byName = byName(underTest.materializeOverlayBlueprints(PATCHED_WITH_737));

        // 7.3.4 and 7.3.5 carry no overlay patch of their own, so fs_trash_interval keeps the 7.3.3 base value.
        assertEquals("0", fsTrashInterval(byName.get(swapPrefix(DE_SPARK3_NAME, "7.3.4")).fileJson()),
                "7.3.4 is an unpatched overlay: fs_trash_interval keeps the base value");
        assertEquals("0", fsTrashInterval(byName.get(swapPrefix(DE_SPARK3_NAME, "7.3.5")).fileJson()),
                "7.3.5 is an unpatched overlay: fs_trash_interval keeps the base value");
        // The 7.3.6 fixture patches fs_trash_interval, and the change forward-propagates into the empty 7.3.7 overlay.
        assertEquals("2880", fsTrashInterval(byName.get(swapPrefix(DE_SPARK3_NAME, "7.3.6")).fileJson()),
                "7.3.6's patch applies to fs_trash_interval");
        assertEquals("2880", fsTrashInterval(byName.get(swapPrefix(DE_SPARK3_NAME, "7.3.7")).fileJson()),
                "the 7.3.6 change must forward-propagate into 7.3.7");
    }

    @Test
    void tombstoneDropsABlueprintFromThatVersionOnward() {
        Map<String, MaterializedBlueprint> byName = byName(underTest.materializeOverlayBlueprints(PATCHED_WITH_736));

        assertNotNull(byName.get(swapPrefix(DATA_MART_NAME, "7.3.5")), "cdp-data-mart exists in 7.3.5 (before the tombstone)");
        assertFalse(byName.containsKey(swapPrefix(DATA_MART_NAME, "7.3.6")), "cdp-data-mart is tombstoned from 7.3.6 onward");
    }

    @Test
    void newBlueprintAddedByAnOverlayIsRegisteredUnderItsSidecarName() {
        Map<String, MaterializedBlueprint> byName = byName(underTest.materializeOverlayBlueprints(PATCHED_WITH_736));

        // cdp-brand-new has no base .bp and no base-block entry; it is added whole at 7.3.6 with a <stem>.name sidecar
        // holding the base-prefixed display name, which the loader prefix-swaps to the overlay version.
        MaterializedBlueprint added = byName.get("7.3.6 - Brand New: Test Service");
        assertNotNull(added, "a brand-new blueprint added by an overlay must register under its sidecar name, prefix-swapped");
        assertEquals("cdp-brand-new", added.fileStem(), "the addition keeps its file stem for the gov-cloud filter");
        assertEquals("7.3.6", added.fileJson().at(CDH_VERSION_POINTER).asText(), "the addition's in-file version fields are injected too");
        assertFalse(byName.containsKey("7.3.4 - Brand New: Test Service"), "an addition anchored at 7.3.6 must not appear in earlier versions");
    }

    @Test
    void newBlueprintWithoutANameSidecarIsSkipped() {
        Map<String, MaterializedBlueprint> byName = byName(underTest.materializeOverlayBlueprints(PATCHED_WITH_736));

        // cdp-orphan is added at 7.3.6 but ships no <stem>.name sidecar and is not in the base block, so there is no DB
        // display name to register it under - it must be skipped rather than materialized under a guessed name.
        assertFalse(byName.values().stream().anyMatch(blueprint -> "cdp-orphan".equals(blueprint.fileStem())),
                "an addition with neither a base-block entry nor a name sidecar must be skipped");
    }

    @Test
    void emptyPatchedSetYieldsNoOverlays() {
        assertTrue(underTest.materializeOverlayBlueprints(Set.of()).isEmpty(), "an empty patched set enumerates no overlay versions");
    }

    private String fsTrashInterval(JsonNode blueprint) {
        JsonNode config = configByName(
                roleConfigGroupByRefName(serviceByRefName(blueprint, "hdfs"), "hdfs-NAMENODE-BASE"),
                "fs_trash_interval");
        return config.path("value").asText();
    }

    private JsonNode serviceByRefName(JsonNode blueprint, String refName) {
        return childMatching(blueprint.at("/blueprint/services"), "refName", refName);
    }

    private JsonNode roleConfigGroupByRefName(JsonNode service, String refName) {
        return childMatching(service.path("roleConfigGroups"), "refName", refName);
    }

    private JsonNode configByName(JsonNode roleConfigGroup, String name) {
        return childMatching(roleConfigGroup.path("configs"), "name", name);
    }

    private JsonNode childMatching(JsonNode array, String field, String value) {
        for (JsonNode element : array) {
            if (value.equals(element.path(field).asText(null))) {
                return element;
            }
        }
        throw new IllegalStateException("No element with " + field + "=" + value);
    }

    private String swapPrefix(String baseName, String version) {
        return version + baseName.substring(BASE_VERSION.length());
    }

    private Map<String, MaterializedBlueprint> byName(List<MaterializedBlueprint> materialized) {
        return materialized.stream().collect(Collectors.toMap(MaterializedBlueprint::name, Function.identity()));
    }

    private JsonNode baseBlueprint(String stem) throws IOException {
        try (InputStream inputStream = new PathMatchingResourcePatternResolver()
                .getResource("classpath:defaults/blueprints/" + BASE_VERSION + "/" + stem + ".bp").getInputStream()) {
            return MAPPER.readTree(inputStream);
        }
    }
}
