package com.sequenceiq.cloudbreak.init.clusterdefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.requests.ClusterDefinitionV4Request;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.AmbariBlueprintUtils;
import com.sequenceiq.cloudbreak.converter.v4.clusterdefinition.ClusterDefinitionV4RequestToClusterDefinitionConverter;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@RunWith(MockitoJUnitRunner.class)
public class DefaulClusterDefinitionCacheTest {

    @Mock
    private AmbariBlueprintUtils ambariBlueprintUtils;

    @Mock
    private ClusterDefinitionV4RequestToClusterDefinitionConverter converter;

    @InjectMocks
    private DefaulClusterDefinitionCache underTest;

    @Test
    public void testEmptyValues() {
        // GIVEN
        underTest.defaultBlueprints().clear();

        Whitebox.setInternalState(underTest, "releasedBlueprints", Collections.singletonList(""));
        Whitebox.setInternalState(underTest, "internalBlueprints", Collections.singletonList(" "));
        Whitebox.setInternalState(underTest, "releasedCMClusterDefinitions", Collections.singletonList(" "));

        // WHEN
        underTest.loadBlueprintsFromFile();
        Map<String, ClusterDefinition> defaultBlueprints = underTest.defaultBlueprints();

        // WHEN
        assertTrue("No cluster definition passed, defaults is expected to be empty", defaultBlueprints.isEmpty());
    }

    @Test
    public void testOnlyReleasedBps() throws IOException {
        // GIVEN
        ClusterDefinition bp1 = new ClusterDefinition();
        bp1.setName("bp1");
        String bp1JsonString = "{\"inputs\":[],\"blueprint\":{\"Blueprints\":{\"blueprint_name\":\"bp1\"}}}";
        JsonNode bpText1 = JsonUtil.readTree(bp1JsonString);
        when(ambariBlueprintUtils.convertStringToJsonNode(any())).thenReturn(bpText1);

        ClusterDefinition bp2 = new ClusterDefinition();
        bp2.setName("bp2");
        String bp2JsonString = "{\"inputs\":[],\"blueprint\":{\"Blueprints\":{\"blueprint_name\":\"bp2\"}}}";
        JsonNode bpText2 = JsonUtil.readTree(bp2JsonString);
        when(ambariBlueprintUtils.convertStringToJsonNode(any())).thenReturn(bpText2);

        when(ambariBlueprintUtils.isBlueprintNamePreConfigured(anyString(), any())).thenReturn(true);
        Whitebox.setInternalState(underTest, "releasedBlueprints", Collections.singletonList("Description1=bp1"));
        Whitebox.setInternalState(underTest, "internalBlueprints", Collections.singletonList(" "));
        Whitebox.setInternalState(underTest, "releasedCMClusterDefinitions", Collections.singletonList("Description2=bp2"));

        when(converter.convert(any(ClusterDefinitionV4Request.class))).thenAnswer(invocation -> {
            ClusterDefinitionV4Request request = invocation.getArgument(0);
            if ("Description1".equalsIgnoreCase(request.getName())) {
                return bp1;
            }
            return bp2;
        });

        // WHEN
        underTest.loadBlueprintsFromFile();
        Map<String, ClusterDefinition> defaultBlueprints = underTest.defaultBlueprints();

        // WHEN
        assertEquals(2L, defaultBlueprints.size());
        assertEquals("Description1", defaultBlueprints.get("bp1").getDescription());
        assertEquals("Description2", defaultBlueprints.get("bp2").getDescription());
    }

}
