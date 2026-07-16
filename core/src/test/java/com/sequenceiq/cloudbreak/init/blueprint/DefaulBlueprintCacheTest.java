package com.sequenceiq.cloudbreak.init.blueprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintV4RequestToBlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintFile;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;
import com.sequenceiq.cloudbreak.service.blueprint.CrnGeneratorService;

@ExtendWith(MockitoExtension.class)
class DefaulBlueprintCacheTest {

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private BlueprintV4RequestToBlueprintConverter converter;

    @Mock
    private BlueprintEntities blueprintEntities;

    @Mock
    private ProviderPreferencesService providerPreferencesService;

    @Mock
    private CommonGovService commonGovService;

    @Mock
    private CrnGeneratorService crnGeneratorService;

    @InjectMocks
    private DefaultBlueprintCache underTest;

    @Test
    void testEmptyValues() {
        when(blueprintEntities.getDefaults()).thenReturn(new HashMap<>());

        // GIVEN
        underTest.defaultBlueprints().clear();

        // WHEN
        underTest.loadBlueprintsFromFile();
        Map<String, BlueprintFile> defaultBlueprints = underTest.defaultBlueprints();

        // WHEN
        assertTrue(defaultBlueprints.isEmpty(), "No blueprint passed, defaults is expected to be empty");
    }

    @Test
    void testOnlyReleasedBps() throws IOException {

        // GIVEN
        Blueprint bp1 = new Blueprint();
        bp1.setName("bp1");
        bp1.setBlueprintText("txt");
        bp1.setStackName("stckn");
        bp1.setStackType("stckt");
        bp1.setStackVersion("7.0.2");
        String bp1JsonString = "{\"inputs\":[],\"blueprint\":{\"Blueprints\":{\"blueprint_name\":\"bp1\"}}}";
        JsonNode bpText1 = JsonUtil.readTree(bp1JsonString);
        when(blueprintUtils.convertStringToJsonNode(any())).thenReturn(bpText1);

        Blueprint bp2 = new Blueprint();
        bp2.setName("bp2");
        bp2.setBlueprintText("txt");
        bp2.setStackName("stckn");
        bp2.setStackType("stckt");
        bp2.setStackVersion("7.0.2");
        String bp2JsonString = "{\"inputs\":[],\"blueprint\":{\"Blueprints\":{\"blueprint_name\":\"bp2\"}}}";
        JsonNode bpText2 = JsonUtil.readTree(bp2JsonString);
        when(blueprintUtils.convertStringToJsonNode(any())).thenReturn(bpText2);

        when(blueprintUtils.isBlueprintNamePreConfigured(anyString(), any())).thenReturn(true);
        when(blueprintEntities.getDefaults()).thenReturn(Map.of("7.11.0", "Description2=bp2;Description1=bp1"));
        when(providerPreferencesService.enabledGovPlatforms()).thenReturn(Set.of(CloudPlatform.AWS.name()));
        when(providerPreferencesService.enabledPlatforms()).thenReturn(Set.of(CloudPlatform.AWS.name()));

        when(converter.convert(any(BlueprintV4Request.class))).thenAnswer(invocation -> {
            BlueprintV4Request request = invocation.getArgument(0);
            if ("Description1".equalsIgnoreCase(request.getName())) {
                return bp1;
            }
            return bp2;
        });
        when(crnGeneratorService.createGlobalDefaultBlueprintCrn(anyString())).thenReturn("crn1", "crn2");

        // WHEN
        underTest.loadBlueprintsFromFile();
        Map<String, BlueprintFile> defaultBlueprints = underTest.defaultBlueprints();

        // WHEN
        assertEquals(2L, defaultBlueprints.size());
        assertEquals("Description1", defaultBlueprints.get("bp1").getDescription());
        assertEquals("Description2", defaultBlueprints.get("bp2").getDescription());
    }

    @Test
    void testLoadBlueprintsFromFileThrowsRuntimeExceptionWhenGeneratedCrnIsDuplicate() throws IOException {

        // GIVEN
        Blueprint bp1 = new Blueprint();
        bp1.setName("bp1");
        bp1.setBlueprintText("txt");
        bp1.setStackName("stckn");
        bp1.setStackType("stckt");
        bp1.setStackVersion("7.0.2");
        String bp1JsonString = "{\"inputs\":[],\"blueprint\":{\"Blueprints\":{\"blueprint_name\":\"bp1\"}}}";
        JsonNode bpText1 = JsonUtil.readTree(bp1JsonString);

        Blueprint bp2 = new Blueprint();
        bp2.setName("bp2");
        bp2.setBlueprintText("txt");
        bp2.setStackName("stckn");
        bp2.setStackType("stckt");
        bp2.setStackVersion("7.0.2");
        String bp2JsonString = "{\"inputs\":[],\"blueprint\":{\"Blueprints\":{\"blueprint_name\":\"bp2\"}}}";
        JsonNode bpText2 = JsonUtil.readTree(bp2JsonString);

        when(blueprintUtils.convertStringToJsonNode(any())).thenReturn(bpText1, bpText2);
        when(blueprintUtils.isBlueprintNamePreConfigured(anyString(), any())).thenReturn(true);
        when(blueprintEntities.getDefaults()).thenReturn(Map.of("7.11.0", "Description2=bp2;Description1=bp1"));

        when(converter.convert(any(BlueprintV4Request.class))).thenAnswer(invocation -> {
            BlueprintV4Request request = invocation.getArgument(0);
            if ("Description1".equalsIgnoreCase(request.getName())) {
                return bp1;
            }
            return bp2;
        });
        when(crnGeneratorService.createGlobalDefaultBlueprintCrn(anyString())).thenReturn("crn");

        // WHEN
        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.loadBlueprintsFromFile());

        // THEN
        assertEquals("crn global default blueprint crn was already generated from another blueprint name.", exception.getMessage());
        assertTrue(underTest.defaultBlueprints().size() < 2, "Loading must stop on duplicate crn before all blueprints are cached");
    }

    @Test
    void testLoadBlueprintsFromFileWithEnums() throws IOException {

        // GIVEN
        Blueprint bp1 = new Blueprint();
        bp1.setName("bp1");
        bp1.setBlueprintText("txt");
        bp1.setStackName("stckn");
        bp1.setStackType("stckt");
        bp1.setStackVersion("7.2.10");
        bp1.setBlueprintUpgradeOption(BlueprintUpgradeOption.DISABLED);
        bp1.setHybridOption(BlueprintHybridOption.BURST_TO_CLOUD);
        String bp1JsonString = "{\"description\":\"7.2.10 - Data Engineering\",\"blueprint\":{\"cdhVersion\":\"7.2.10\",\"displayName\":\"dataengineering\","
                + "\"blueprintUpgradeOption\":\"DISABLED\", \"hybridOption\": \"BURST_TO_CLOUD\"}}";
        JsonNode bpText1 = JsonUtil.readTree(bp1JsonString);
        when(blueprintUtils.convertStringToJsonNode(any())).thenReturn(bpText1);
        when(blueprintEntities.getDefaults()).thenReturn(Map.of("7.2.10", "Description1=bp1"));
        when(blueprintUtils.isBlueprintNamePreConfigured(anyString(), any())).thenReturn(true);

        when(converter.convert(any(BlueprintV4Request.class))).thenAnswer(invocation -> {
            BlueprintV4Request request = invocation.getArgument(0);
            return bp1;
        });
        when(crnGeneratorService.createGlobalDefaultBlueprintCrn(anyString())).thenReturn("crn");

        underTest.loadBlueprintsFromFile();

        Map<String, BlueprintFile> defaultBlueprints = underTest.defaultBlueprints();
        assertEquals(1L, defaultBlueprints.size());
        BlueprintFile blueprintFile = defaultBlueprints.get("bp1");
        assertEquals("7.2.10 - Data Engineering", blueprintFile.getDescription());
        assertEquals(BlueprintUpgradeOption.DISABLED, blueprintFile.getBlueprintUpgradeOption());
        assertEquals(BlueprintHybridOption.BURST_TO_CLOUD, blueprintFile.getHybridOption());
    }

    @Test
    void testGetBlueprintVersions() {
        // GIVEN
        BlueprintFile bp1 = mock(BlueprintFile.class);
        BlueprintFile bp2 = mock(BlueprintFile.class);
        when(bp1.getStackVersion()).thenReturn("7.2.10");
        when(bp2.getStackVersion()).thenReturn("7.2.15");

        underTest.defaultBlueprints().put("bp1", bp1);
        underTest.defaultBlueprints().put("bp2", bp2);

        // WHEN
        Set<String> versions = underTest.getBlueprintVersions();

        // THEN
        assertEquals(2, versions.size());
        assertTrue(versions.contains("7.2.10"));
        assertTrue(versions.contains("7.2.15"));
    }

    @Test
    void testGetDefaultByName() {
        // GIVEN
        BlueprintFile bp1 = mock(BlueprintFile.class);
        BlueprintFile bp2 = mock(BlueprintFile.class);

        underTest.defaultBlueprints().put("bp1", bp1);
        underTest.defaultBlueprints().put("bp2", bp2);

        // WHEN & THEN
        assertEquals(bp1, underTest.getDefaultByName("bp1").get());
        assertEquals(bp2, underTest.getDefaultByName("bp2").get());
        assertFalse(underTest.getDefaultByName("unknown").isPresent());
    }

    @Test
    void testIsDefaultByCrn() {
        // GIVEN
        BlueprintFile bp1 = mock(BlueprintFile.class);
        BlueprintFile bp2 = mock(BlueprintFile.class);
        when(bp1.getResourceCrn()).thenReturn("crn-bp1");
        when(bp2.getResourceCrn()).thenReturn("crn-bp2");

        underTest.defaultBlueprints().put("bp1", bp1);
        underTest.defaultBlueprints().put("bp2", bp2);

        // WHEN & THEN
        assertTrue(underTest.isDefaultByCrn("crn-bp1"));
        assertTrue(underTest.isDefaultByCrn("crn-bp2"));
        assertFalse(underTest.isDefaultByCrn("crn-unknown"));
    }

    @Test
    void testGetDefaultByCrn() {
        // GIVEN
        BlueprintFile bp1 = mock(BlueprintFile.class);
        BlueprintFile bp2 = mock(BlueprintFile.class);
        when(bp1.getResourceCrn()).thenReturn("crn-bp1");
        lenient().when(bp2.getResourceCrn()).thenReturn("crn-bp2");

        underTest.defaultBlueprints().put("bp1", bp1);
        underTest.defaultBlueprints().put("bp2", bp2);

        // WHEN
        BlueprintFile result = underTest.getDefaultByCrn("crn-bp1");

        // THEN
        assertEquals(bp1, result);
    }

    @Test
    void testGetDefaultByCrnThrowsNotFoundException() {
        // GIVEN
        BlueprintFile bp1 = mock(BlueprintFile.class);
        when(bp1.getResourceCrn()).thenReturn("crn-bp1");
        underTest.defaultBlueprints().put("bp1", bp1);

        // WHEN & THEN
        assertThrows(NotFoundException.class, () -> underTest.getDefaultByCrn("crn-unknown"));
    }
}