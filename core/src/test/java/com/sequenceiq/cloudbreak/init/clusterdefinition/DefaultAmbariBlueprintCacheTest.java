package com.sequenceiq.cloudbreak.init.clusterdefinition;

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
import com.sequenceiq.cloudbreak.converter.v4.blueprints.BlueprintV4RequestToBlueprintConverter;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.AmbariBlueprintUtils;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class DefaultAmbariBlueprintCacheTest {

    @Mock
    private AmbariBlueprintUtils ambariBlueprintUtils;

    @Mock
    private BlueprintV4RequestToBlueprintConverter converter;

    @InjectMocks
    private DefaultAmbariBlueprintCache underTest;

    @Before
    public void setup() throws IOException {

        ClusterDefinition bp1 = new ClusterDefinition();
        bp1.setName("bp1");

        JsonNode bpText = JsonUtil.readTree("{\"inputs\":[],\"blueprint\":{\"Blueprints\":{\"blueprint_name\":\"bp1\"}}}");

        when(ambariBlueprintUtils.isBlueprintNamePreConfigured(anyString(), any())).thenReturn(true);
        when(ambariBlueprintUtils.convertStringToJsonNode(any())).thenReturn(bpText);
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
        Map<String, ClusterDefinition> defaultBlueprints = underTest.defaultBlueprints();

        // WHEN
        assertTrue("No cluster definition passed, defaults is expected to be empty", defaultBlueprints.isEmpty());
    }

    @Test
    public void testOnlyReleasedBps() {
        // GIVEN
        Whitebox.setInternalState(underTest, "releasedBlueprints", Collections.singletonList("Description1=bp1"));
        Whitebox.setInternalState(underTest, "internalBlueprints", Collections.singletonList(" "));

        // WHEN
        underTest.loadBlueprintsFromFile();
        Map<String, ClusterDefinition> defaultBlueprints = underTest.defaultBlueprints();

        // WHEN
        assertEquals(1L, defaultBlueprints.size());
        assertEquals("Description1", defaultBlueprints.get("bp1").getDescription());
    }

}
