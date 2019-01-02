package com.sequenceiq.cloudbreak.init.blueprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.converter.v4.blueprints.BlueprintV4RequestToBlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class DefaultBlueprintCacheTest {

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private BlueprintV4RequestToBlueprintConverter converter;

    @InjectMocks
    private DefaultBlueprintCache underTest;

    @Before
    public void setup() throws IOException {

        Blueprint bp1 = new Blueprint();
        bp1.setName("bp1");

        JsonNode bpText = JsonUtil.readTree("{\"inputs\":[],\"blueprint\":{\"Blueprints\":{\"blueprint_name\":\"bp1\"}}}");

        when(blueprintUtils.isBlueprintNamePreConfigured(anyString(), any())).thenReturn(true);
        when(blueprintUtils.convertStringToJsonNode(any())).thenReturn(bpText);
        when(converter.convert(any(BlueprintV4Request.class))).thenReturn(bp1);

        underTest.defaultBlueprints().clear();
    }

    @Test
    public void testEmptyValues() {
        // GIVEN
        Whitebox.setInternalState(underTest, "releasedBlueprints", Collections.singletonList(""));
        Whitebox.setInternalState(underTest, "internalBlueprints", Collections.singletonList(" "));

        // WHEN
        underTest.loadBlueprintsFromFile();
        Map<String, Blueprint> defaultBlueprints = underTest.defaultBlueprints();

        // WHEN
        assertTrue("No blueprint passed, defaults is expected to be empty", defaultBlueprints.isEmpty());
    }

    @Test
    public void testOnlyReleasedBps() {
        // GIVEN
        Whitebox.setInternalState(underTest, "releasedBlueprints", Collections.singletonList("Description1=bp1"));
        Whitebox.setInternalState(underTest, "internalBlueprints", Collections.singletonList(" "));

        // WHEN
        underTest.loadBlueprintsFromFile();
        Map<String, Blueprint> defaultBlueprints = underTest.defaultBlueprints();

        // WHEN
        assertEquals(1L, defaultBlueprints.size());
        assertEquals("Description1", defaultBlueprints.get("bp1").getDescription());
    }

}
