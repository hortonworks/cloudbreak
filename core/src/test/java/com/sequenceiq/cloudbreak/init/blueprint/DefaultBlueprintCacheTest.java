package com.sequenceiq.cloudbreak.init.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintV4RequestToBlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintFile;
import com.sequenceiq.cloudbreak.init.blueprint.overlay.RuntimeBlueprintOverlayLoader;
import com.sequenceiq.cloudbreak.init.blueprint.overlay.RuntimeBlueprintOverlayLoader.MaterializedBlueprint;
import com.sequenceiq.cloudbreak.service.blueprint.CrnGeneratorService;

/**
 * Focuses on the overlay-merge branches {@link DefaultBlueprintCache} gained for the runtime base+overlay model:
 * a materialized overlay blueprint is registered under its synthesized name, a real on-disk blueprint of the same
 * name always wins, an empty patched set adds nothing, and the gov-cloud exclusion filter is applied to overlays
 * exactly as it is to on-disk blueprints. The on-disk block is left empty so these branches are exercised in
 * isolation (the disk scan itself is covered by the integration tests that load the real 7.3.3 block).
 */
@ExtendWith(MockitoExtension.class)
class DefaultBlueprintCacheTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String OVERLAY_NAME = "7.3.4 - Data Engineering: Apache Spark3, Apache Hive, Apache Oozie";

    private static final String OVERLAY_STEM = "cdp-data-engineering-spark3";

    @InjectMocks
    private DefaultBlueprintCache underTest;

    @Mock
    private BlueprintEntities blueprintEntities;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private BlueprintV4RequestToBlueprintConverter converter;

    @Mock
    private ProviderPreferencesService preferencesService;

    @Mock
    private CommonGovService commonGovService;

    @Mock
    private GovCloudExclusionFilter govCloudExculsionFilter;

    @Mock
    private CrnGeneratorService crnGeneratorService;

    @Mock
    private RuntimeBlueprintOverlayLoader runtimeBlueprintOverlayLoader;

    @BeforeEach
    void setUp() throws IOException {
        // Empty on-disk block: blueprints() filters out the blank value, so the disk loop is a no-op and only the
        // overlay-merge path under test runs.
        lenient().when(blueprintEntities.getDefaults()).thenReturn(Map.of("7.3.3", ""));
        lenient().doAnswer(invocation -> invocation.getArgument(0)).when(crnGeneratorService).createGlobalDefaultBlueprintCrn(anyString());
        lenient().when(blueprintUtils.prepareTags(any())).thenReturn(Map.of());
        lenient().when(converter.convert(any(BlueprintV4Request.class))).thenAnswer(invocation -> {
            BlueprintV4Request request = invocation.getArgument(0);
            Blueprint blueprint = new Blueprint();
            blueprint.setName(request.getName());
            blueprint.setBlueprintText(request.getBlueprint());
            blueprint.setStackName("CDP 7.3.4");
            blueprint.setStackType("CDH");
            blueprint.setStackVersion("7.3.4");
            return blueprint;
        });
        underTest.setRuntimeOverlayEnabled(true);
    }

    @Test
    void registersOverlayWhenNoOnDiskBlueprintOfThatName() {
        when(runtimeBlueprintOverlayLoader.materializeOverlayBlueprints(any())).thenReturn(List.of(overlay(OVERLAY_NAME, OVERLAY_STEM)));
        underTest.setPatchedRuntimes(List.of("7.3.4"));

        underTest.loadBlueprintsFromFile();

        Map<String, BlueprintFile> actual = underTest.defaultBlueprints();
        assertEquals(1, actual.size());
        assertNotNull(actual.get(OVERLAY_NAME), "the overlay blueprint must be registered under its synthesized name");
    }

    @Test
    void keepsOnDiskBlueprintOverAnOverlayWithTheSameName() {
        BlueprintFile onDisk = new BlueprintFile.Builder().name(OVERLAY_NAME).blueprintText("on-disk").stackName("CDP 7.3.4")
                .stackVersion("7.3.4").stackType("CDH").resourceCrn(OVERLAY_NAME).build();
        underTest.defaultBlueprints().put(OVERLAY_NAME, onDisk);
        when(runtimeBlueprintOverlayLoader.materializeOverlayBlueprints(any())).thenReturn(List.of(overlay(OVERLAY_NAME, OVERLAY_STEM)));
        underTest.setPatchedRuntimes(List.of("7.3.4"));

        underTest.loadBlueprintsFromFile();

        assertEquals(1, underTest.defaultBlueprints().size(), "the overlay of an existing name must not add an entry");
        assertSame(onDisk, underTest.defaultBlueprints().get(OVERLAY_NAME), "the on-disk blueprint must win over the overlay of the same name");
    }

    @Test
    void addsNothingWhenNoOverlaysAreMaterialized() {
        when(runtimeBlueprintOverlayLoader.materializeOverlayBlueprints(any())).thenReturn(List.of());
        underTest.setPatchedRuntimes(List.of("7.3.4"));

        underTest.loadBlueprintsFromFile();

        assertTrue(underTest.defaultBlueprints().isEmpty(), "an empty overlay set adds no blueprints");
    }

    @Test
    void appliesTheGovExclusionFilterToOverlaysAndDropsExcludedOnes() {
        when(commonGovService.govCloudDeployment(any(), any())).thenReturn(true);
        when(govCloudExculsionFilter.shouldAddBlueprint(eq("7.3.4"), eq(OVERLAY_STEM))).thenReturn(false);
        when(runtimeBlueprintOverlayLoader.materializeOverlayBlueprints(any())).thenReturn(List.of(overlay(OVERLAY_NAME, OVERLAY_STEM)));
        underTest.setPatchedRuntimes(List.of("7.3.4"));

        underTest.loadBlueprintsFromFile();

        assertTrue(underTest.defaultBlueprints().isEmpty(), "a gov-excluded overlay must be dropped, exactly as an on-disk blueprint would be");
    }

    @Test
    void keepsOverlayInGovDeploymentWhenTheFilterAllowsIt() {
        when(commonGovService.govCloudDeployment(any(), any())).thenReturn(true);
        when(govCloudExculsionFilter.shouldAddBlueprint(eq("7.3.4"), eq(OVERLAY_STEM))).thenReturn(true);
        when(runtimeBlueprintOverlayLoader.materializeOverlayBlueprints(any())).thenReturn(List.of(overlay(OVERLAY_NAME, OVERLAY_STEM)));
        underTest.setPatchedRuntimes(List.of("7.3.4"));

        underTest.loadBlueprintsFromFile();

        assertNotNull(underTest.defaultBlueprints().get(OVERLAY_NAME), "a gov-allowed overlay must be registered");
    }

    @Test
    void addsNoOverlayWhenTheKillSwitchIsOff() {
        underTest.setRuntimeOverlayEnabled(false);
        underTest.setPatchedRuntimes(List.of("7.3.4"));

        underTest.loadBlueprintsFromFile();

        assertTrue(underTest.defaultBlueprints().isEmpty(), "with the overlay kill-switch off no overlay must be materialized");
    }

    private MaterializedBlueprint overlay(String name, String stem) {
        try {
            JsonNode fileJson = MAPPER.readTree("{\"description\":\"" + name + "\",\"blueprint\":{\"cdhVersion\":\"7.3.4\"}}");
            return new MaterializedBlueprint(name, stem, fileJson);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
