package com.sequenceiq.cloudbreak.shell.transformer;

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.shell.model.OutPutType;
import com.sequenceiq.cloudbreak.shell.support.JsonRenderer;
import com.sequenceiq.cloudbreak.shell.support.TableRenderer;

public class OutputTransformerTest {

    @InjectMocks
    private OutputTransformer underTest;

    @Spy
    private TableRenderer tableRenderer = new TableRenderer();

    @Mock
    private JsonRenderer jsonRenderer;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void renderMapWithNullFirstValue() throws Exception {
        Map<String, String> map = Maps.newTreeMap();
        map.put("subnetCIDR", null);
        map.put("description", null);
        map.put("name", "net1479586335554335717");
        map.put("cloudPlatform", "AWS");
        map.put("topologyId", null);
        map.put("id", "42");
        map.put("publicInAccount", "true");

        String expectedResult = new TableRenderer().renderSingleMapWithSortedColumn(map, "FIELD", "VALUE");
        String actualResult = underTest.render(OutPutType.RAW, map, "FIELD", "VALUE");

        Assert.assertEquals(expectedResult, actualResult);
    }
}
