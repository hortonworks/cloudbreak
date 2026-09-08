package com.sequenceiq.datalake.configuration.overlay;

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
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.runtime.overlay.RuntimeOverlayConstants;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.datalake.service.sdx.CDPConfigKey;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

/**
 * Exercises {@link RuntimeDutyOverlayLoader} against the real overlay resources committed under
 * {@code datalake/src/main/resources/runtime-overlays/} and the frozen base duties on disk. It proves
 * the end-to-end base+overlay path the loader adds:
 * <ul>
 *   <li>7.3.4 and 7.3.5 are materialized entirely from the 7.3.3 base — no full duty dir on disk and
 *       (as shipped) no overlay patch of their own, so they equal the base modulo version injection,</li>
 *   <li>a name-addressed instance-type patch (the test-only 7.3.6 fixture) changes only the {@code core}
 *       host group, and</li>
 *   <li>that change forward-propagates into the empty 7.3.7 overlay above it.</li>
 * </ul>
 * <p>Chain mechanics that need two patch anchors (highest-anchor-wins on a shared path) are proven generically
 * for the shared resolver in {@code RuntimeOverlayResolverTest}; this test focuses on the duty-specific wiring.
 */
class RuntimeDutyOverlayLoaderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Set<String> SUPPORTED = Set.of("7.3.2", "7.3.3", "7.3.4", "7.3.5");

    // Adds 7.3.6, which exists only as a test-only overlay under src/test/resources/runtime-overlays/7.3.6:
    // it patches aws/medium_duty_ha's core group, tombstones aws/light_duty, and adds aws/containerized.
    private static final Set<String> SUPPORTED_WITH_736 = Set.of("7.3.2", "7.3.3", "7.3.4", "7.3.5", "7.3.6");

    // Adds an empty 7.3.7 overlay (no dir at all) above 7.3.6 to prove the 7.3.6 patch forward-propagates.
    private static final Set<String> SUPPORTED_WITH_737 = Set.of("7.3.2", "7.3.3", "7.3.4", "7.3.5", "7.3.6", "7.3.7");

    private final RuntimeDutyOverlayLoader underTest = new RuntimeDutyOverlayLoader();

    @Test
    void materializesOnlyTheOverlayVersionsAboveTheBase() {
        Map<CDPConfigKey, String> materialized = underTest.materializeOverlayDuties(SUPPORTED);

        assertFalse(materialized.isEmpty(), "expected overlay versions to be materialized");
        // The base and everything on disk are served by CDPConfigService's own scan, not the overlay loader.
        assertTrue(materialized.keySet().stream().noneMatch(key -> "7.3.2".equals(key.getRuntimeVersion())),
                "on-disk 7.3.2 must not be materialized as an overlay");
        assertTrue(materialized.keySet().stream().noneMatch(key -> RuntimeOverlayConstants.BASE_VERSION.equals(key.getRuntimeVersion())),
                "the base 7.3.3 (full dir on disk) must not be materialized as an overlay");
        assertTrue(materialized.keySet().stream().anyMatch(key -> "7.3.4".equals(key.getRuntimeVersion())), "7.3.4 should be materialized");
        assertTrue(materialized.keySet().stream().anyMatch(key -> "7.3.5".equals(key.getRuntimeVersion())), "7.3.5 should be materialized");

        long count734 = materialized.keySet().stream().filter(key -> "7.3.4".equals(key.getRuntimeVersion())).count();
        long count735 = materialized.keySet().stream().filter(key -> "7.3.5".equals(key.getRuntimeVersion())).count();
        assertEquals(count734, count735, "7.3.4 and 7.3.5 cover the same duty set (no adds/removes between them)");
    }

    @Test
    void instanceTypePatchAppliesToTheNamedHostGroupAndForwardPropagates() throws IOException {
        Map<CDPConfigKey, String> materialized = underTest.materializeOverlayDuties(SUPPORTED_WITH_737);

        // 7.3.4 and 7.3.5 carry no overlay patch of their own, so the core group keeps the 7.3.3 base value.
        assertEquals("m5.2xlarge", instanceType(materialized, "7.3.4", "core"), "7.3.4 is an unpatched overlay: core keeps the base instance type");
        assertEquals("m5.2xlarge", instanceType(materialized, "7.3.5", "core"), "7.3.5 is an unpatched overlay: core keeps the base instance type");
        // The 7.3.6 fixture patch addresses /instanceGroups/name=core/... so only the core group changes.
        assertEquals("m5.8xlarge", instanceType(materialized, "7.3.6", "core"), "7.3.6's patch applies to the named core group");
        // ... and the change forward-propagates into the empty 7.3.7 overlay above it.
        assertEquals("m5.8xlarge", instanceType(materialized, "7.3.7", "core"), "the 7.3.6 change must forward-propagate into 7.3.7");
        // Other groups in the same duty are untouched by the patch.
        assertEquals("m5.xlarge", instanceType(materialized, "7.3.6", "master"), "an unpatched host group must keep the base instance type");
    }

    @Test
    void unpatchedDutyIsPureBasePlusVersionInjection() throws IOException {
        Map<CDPConfigKey, String> materialized = underTest.materializeOverlayDuties(SUPPORTED);
        CDPConfigKey lightDuty735 = new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.3.5", Architecture.X86_64);

        String raw = materialized.get(lightDuty735);
        assertNotNull(raw, "aws light_duty must be materialized for 7.3.5");
        String blueprintName = MAPPER.readTree(raw).at("/cluster/blueprintName").asText();
        assertTrue(blueprintName.startsWith("7.3.5 "), "version fields must be injected for 7.3.5, got: " + blueprintName);
    }

    @Test
    void emptySupportedSetYieldsNoOverlays() {
        assertTrue(underTest.materializeOverlayDuties(Set.of()).isEmpty(), "an empty supported set enumerates no overlay versions");
    }

    @Test
    void tombstoneDropsADutyFromThatVersionOnward() {
        Map<CDPConfigKey, String> materialized = underTest.materializeOverlayDuties(SUPPORTED_WITH_736);
        CDPConfigKey lightDuty735 = new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.3.5", Architecture.X86_64);
        CDPConfigKey lightDuty736 = new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.3.6", Architecture.X86_64);

        assertNotNull(materialized.get(lightDuty735), "aws light_duty exists in 7.3.5 (before the tombstone)");
        assertFalse(materialized.containsKey(lightDuty736), "aws light_duty is tombstoned from 7.3.6 onward");
    }

    @Test
    void newDutyAddedByAnOverlayIsMaterializedUnderItsDerivedConfigKey() throws IOException {
        Map<CDPConfigKey, String> materialized = underTest.materializeOverlayDuties(SUPPORTED_WITH_736);
        // CONTAINERIZED has no base 7.3.3 duty file for any platform; aws/containerized.json is added whole at 7.3.6.
        CDPConfigKey containerized736 = new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.CONTAINERIZED, "7.3.6", Architecture.X86_64);
        CDPConfigKey containerized734 = new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.CONTAINERIZED, "7.3.4", Architecture.X86_64);

        String raw = materialized.get(containerized736);
        assertNotNull(raw, "a brand-new duty added by an overlay must materialize under its path-derived CDPConfigKey");
        assertTrue(MAPPER.readTree(raw).at("/cluster/blueprintName").asText().startsWith("7.3.6 "),
                "the addition's version field must be injected to the overlay version");
        assertFalse(materialized.containsKey(containerized734), "an addition anchored at 7.3.6 must not appear in earlier versions");
    }

    @Test
    void patchedDutyDiffersFromTheBaseOnlyAtTheVersionFieldAndTheTargetedInstanceType() throws IOException {
        Map<CDPConfigKey, String> materialized = underTest.materializeOverlayDuties(SUPPORTED_WITH_736);
        JsonNode base = baseDuty("aws/medium_duty_ha.json");
        JsonNode materialized736 = mediumDutyHa(materialized, "7.3.6");

        // The whole promise of the model: a materialized overlay is byte-for-byte the base except for
        // the fields the version injection and the single 7.3.6 patch legitimately touch - nothing else.
        List<String> differences = JsonTreeAssertions.differingPaths(base, materialized736);

        assertEquals(2, differences.size(), "exactly the version field and one instance type should differ, got: " + differences);
        assertTrue(differences.contains("/cluster/blueprintName"), "the injected version field must differ, got: " + differences);
        assertTrue(differences.stream().anyMatch(path -> path.endsWith("/template/instanceType")),
                "the one patched instance type must differ, got: " + differences);
    }

    @Test
    void unpatchedDutyEqualsTheBaseModuloTheInjectedVersionField() throws IOException {
        Map<CDPConfigKey, String> materialized = underTest.materializeOverlayDuties(SUPPORTED);
        JsonNode base = baseDuty("aws/light_duty.json");
        CDPConfigKey lightDuty734 = new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.3.4", Architecture.X86_64);
        JsonNode materialized734 = MAPPER.readTree(materialized.get(lightDuty734));

        // aws/light_duty carries no patch at 7.3.4, so it must reconstruct as pure base + version injection.
        JsonTreeAssertions.assertEqualsIgnoringPaths(base, materialized734, Set.of("/cluster/blueprintName"),
                "an unpatched duty must equal the 7.3.3 base once the injected version field is excused");
    }

    private JsonNode baseDuty(String relativePath) throws IOException {
        try (InputStream inputStream = new ClassPathResource("duties/" + RuntimeOverlayConstants.BASE_VERSION + "/" + relativePath).getInputStream()) {
            return MAPPER.readTree(inputStream);
        }
    }

    private JsonNode mediumDutyHa(Map<CDPConfigKey, String> materialized, String version) throws IOException {
        CDPConfigKey key = new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, version, Architecture.X86_64);
        String raw = materialized.get(key);
        assertNotNull(raw, "aws medium_duty_ha must be materialized for " + version);
        return MAPPER.readTree(raw);
    }

    private String instanceType(Map<CDPConfigKey, String> materialized, String version, String groupName) throws IOException {
        CDPConfigKey key = new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, version, Architecture.X86_64);
        String raw = materialized.get(key);
        assertNotNull(raw, "aws medium_duty_ha must be materialized for " + version);
        for (JsonNode group : MAPPER.readTree(raw).path("instanceGroups")) {
            if (groupName.equals(group.path("name").asText())) {
                return group.at("/template/instanceType").asText();
            }
        }
        throw new IllegalStateException("host group " + groupName + " not found in materialized " + version + " medium_duty_ha");
    }
}
