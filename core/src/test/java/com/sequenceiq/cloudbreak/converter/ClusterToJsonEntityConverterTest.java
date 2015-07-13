package com.sequenceiq.cloudbreak.converter;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.controller.json.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.controller.json.ClusterResponse;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.domain.AmbariStackDetails;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;

public class ClusterToJsonEntityConverterTest extends AbstractEntityConverterTest<Cluster> {

    @InjectMocks
    private ClusterToJsonConverter underTest;

    @Mock
    private BlueprintValidator blueprintValidator;
    @Mock
    private StackServiceComponentDescriptors stackServiceComponentDescs;
    @Mock
    private ConversionService conversionService;
    @Mock
    private JsonNode jsonNode;
    @Mock
    private JsonNode nameJsonNode;
    @Mock
    private Iterator<JsonNode> mockIterator;
    @Mock
    private Map<String, HostGroup> hostGroupMap;
    @Mock
    private HostGroup hostGroup;
    @Mock
    private InstanceGroup instanceGroup;
    @Mock
    private InstanceMetaData instanceMetaData;
    @Mock
    private Iterator<JsonNode> mockComponentIterator;

    private StackServiceComponentDescriptor stackServiceComponentDescriptor;

    @Before
    public void setUp() {
        underTest = new ClusterToJsonConverter();
        MockitoAnnotations.initMocks(this);
        stackServiceComponentDescriptor = createStackServiceComponentDescriptor();

    }

    @Test
    public void testConvert() throws IOException {
        // GIVEN
        mockAll();
        given(stackServiceComponentDescs.get(anyString())).willReturn(stackServiceComponentDescriptor);
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, (long) result.getId());
        assertNotNull(result.getAmbariStackDetails());
        assertAllFieldsNotNull(result, Arrays.asList("cluster"));
    }

    @Test
    public void testConvertWithoutAmbariStackDetails() throws IOException {
        // GIVEN
        mockAll();
        getSource().setAmbariStackDetails(null);
        given(stackServiceComponentDescs.get(anyString())).willReturn(stackServiceComponentDescriptor);
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, (long) result.getId());
        assertNull(result.getAmbariStackDetails());
        assertAllFieldsNotNull(result, Arrays.asList("ambariStackDetails", "cluster"));
    }

    @Test
    public void testConvertWithoutUpSinceField() throws IOException {
        // GIVEN
        mockAll();
        getSource().setUpSince(null);
        given(stackServiceComponentDescs.get(anyString())).willReturn(stackServiceComponentDescriptor);
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(0L, (long) result.getMinutesUp());
    }

    @Test
    public void testConvertWhenBaywatchEnabled() throws IOException {
        // GIVEN
        mockAll();
        given(stackServiceComponentDescs.get(anyString())).willReturn(stackServiceComponentDescriptor);
        ReflectionTestUtils.setField(underTest, "baywatchEnabled", true);
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals("10.0.0.1:3080", result.getServiceEndPoints().get("Kibana"));
    }

    @Test
    public void testConvertWithoutMasterComponent() throws IOException {
        // GIVEN
        mockAll();
        given(stackServiceComponentDescs.get(anyString())).willReturn(new StackServiceComponentDescriptor("dummy", "dummy", 1));
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, (long) result.getId());
        assertNotNull(result.getAmbariStackDetails());
    }

    @Test
    public void testConvertWithGangliaComponent() throws IOException {
        // GIVEN
        mockAll();
        given(instanceMetaData.getPublicIp()).willReturn("dummyPublicIp");
        given(stackServiceComponentDescs.get(anyString())).willReturn(new StackServiceComponentDescriptor("GANGLIA_SERVER", "MASTER", 1));
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals("dummyPublicIp/ganglia", result.getServiceEndPoints().get("Ganglia"));
    }

    @Test
    public void testConvertWhenValidatorThrowException() throws IOException {
        // GIVEN
        given(blueprintValidator.getHostGroupNode(any(Blueprint.class))).willThrow(new IOException("error"));
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        verify(blueprintValidator, times(0)).createHostGroupMap(anySet());

    }

    @Override
    public Cluster createSource() {
        Stack stack = TestUtil.stack();
        Blueprint blueprint = TestUtil.blueprint();
        return TestUtil.cluster(blueprint, stack, 1L);
    }

    private void mockAll() throws IOException {
        given(blueprintValidator.getHostGroupNode(any(Blueprint.class))).willReturn(jsonNode);
        given(jsonNode.iterator()).willReturn(mockIterator);
        given(mockIterator.hasNext()).willReturn(true).willReturn(false);
        given(mockIterator.next()).willReturn(jsonNode);
        given(conversionService.convert(any(AmbariStackDetails.class), any(Class.class))).willReturn(new AmbariStackDetailsJson());
        given(blueprintValidator.getHostGroupName(jsonNode)).willReturn("slave_1");
        given(blueprintValidator.createHostGroupMap(any(Set.class))).willReturn(hostGroupMap);
        given(hostGroupMap.get("slave_1")).willReturn(hostGroup);
        given(hostGroup.getInstanceGroup()).willReturn(instanceGroup);
        given(instanceGroup.getInstanceMetaData()).willReturn(Sets.newHashSet(instanceMetaData));
        given(blueprintValidator.getComponentsNode(jsonNode)).willReturn(nameJsonNode);
        given(nameJsonNode.iterator()).willReturn(mockComponentIterator);
        given(mockComponentIterator.hasNext()).willReturn(true).willReturn(false);
        given(mockComponentIterator.next()).willReturn(nameJsonNode);
        given(nameJsonNode.get(anyString())).willReturn(nameJsonNode);
        given(nameJsonNode.asText()).willReturn("dummyName");
    }

    private StackServiceComponentDescriptor createStackServiceComponentDescriptor() {
        return new StackServiceComponentDescriptor("ELASTIC_SEARCH", "MASTER", 1);
    }
}
