package com.sequenceiq.cloudbreak.init.blueprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintV4RequestToBlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintFile;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;

@RunWith(MockitoJUnitRunner.class)
public class DefaulBlueprintCacheTest {

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

    @InjectMocks
    private DefaultBlueprintCache underTest;

    @Test
    public void testEmptyValues() {
        when(blueprintEntities.getDefaults()).thenReturn(new HashMap<>());

        // GIVEN
        underTest.defaultBlueprints().clear();

        // WHEN
        underTest.loadBlueprintsFromFile();
        Map<String, BlueprintFile> defaultBlueprints = underTest.defaultBlueprints();

        // WHEN
        assertTrue("No blueprint passed, defaults is expected to be empty", defaultBlueprints.isEmpty());
    }

    @Test
    public void testOnlyReleasedBps() throws IOException {

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

        // WHEN
        underTest.loadBlueprintsFromFile();
        Map<String, BlueprintFile> defaultBlueprints = underTest.defaultBlueprints();

        // WHEN
        assertEquals(2L, defaultBlueprints.size());
        assertEquals("Description1", defaultBlueprints.get("bp1").getDescription());
        assertEquals("Description2", defaultBlueprints.get("bp2").getDescription());
    }

    @Test
    public void testLoadBlueprintsFromFileWithEnums() throws IOException {

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

        underTest.loadBlueprintsFromFile();

        Map<String, BlueprintFile> defaultBlueprints = underTest.defaultBlueprints();
        assertEquals(1L, defaultBlueprints.size());
        BlueprintFile blueprintFile = defaultBlueprints.get("bp1");
        assertEquals("7.2.10 - Data Engineering", blueprintFile.getDescription());
        assertEquals(BlueprintUpgradeOption.DISABLED, blueprintFile.getBlueprintUpgradeOption());
        assertEquals(BlueprintHybridOption.BURST_TO_CLOUD, blueprintFile.getHybridOption());
    }

}
