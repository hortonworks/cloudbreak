package com.sequenceiq.cloudbreak.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.ConversionService;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.RDSConfigRequest;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.BlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorType;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.SssdConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.cluster.flow.AmbariViewProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;

public class ClusterToJsonConverterTest extends AbstractEntityConverterTest<Cluster> {

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

    @Mock
    private AmbariViewProvider ambariViewProvider;

    @Mock
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private RdsConfigService rdsConfigService;

    private StackServiceComponentDescriptor stackServiceComponentDescriptor;

    @Before
    public void setUp() throws CloudbreakException {
        underTest = new ClusterToJsonConverter();
        MockitoAnnotations.initMocks(this);
        given(orchestratorTypeResolver.resolveType(any(Orchestrator.class))).willReturn(OrchestratorType.HOST);
        given(rdsConfigService.findByClusterId(anyString(), anyString(), anyLong())).willReturn(new HashSet<>());
        stackServiceComponentDescriptor = createStackServiceComponentDescriptor();
    }

    @Test
    public void testConvert() throws IOException {
        // GIVEN
        mockAll();
        getSource().setConfigStrategy(ConfigStrategy.NEVER_APPLY);
        given(stackServiceComponentDescs.get(anyString())).willReturn(stackServiceComponentDescriptor);
        given(stackUtil.extractAmbariIp(any(Stack.class))).willReturn("10.0.0.1");
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, (long) result.getId());
        assertAllFieldsNotNull(result, Lists.newArrayList("cluster", "ambariStackDetails", "rdsConfigId", "blueprintCustomProperties",
                "blueprint", "sssdConfig", "rdsConfigs", "ldapConfig", "exposedKnoxServices", "customContainers",
                "ambariRepoDetailsJson", "ambariDatabaseDetails"));
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
    public void testConvertWithoutMasterComponent() throws IOException {
        // GIVEN
        mockAll();
        given(stackServiceComponentDescs.get(anyString())).willReturn(new StackServiceComponentDescriptor("dummy", "dummy", 1, 1));
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, (long) result.getId());
    }

    @Test
    public void testConvertWhenValidatorThrowException() throws IOException {
        // GIVEN
        given(blueprintValidator.getHostGroupNode(any(Blueprint.class))).willThrow(new IOException("error"));
        // WHEN
        underTest.convert(getSource());
        // THEN
        verify(blueprintValidator, times(0)).createHostGroupMap(anySet());

    }

    @Override
    public Cluster createSource() {
        Stack stack = TestUtil.stack();
        Blueprint blueprint = TestUtil.blueprint();
        SssdConfig config = TestUtil.sssdConfigs(1).iterator().next();
        Cluster cluster = TestUtil.cluster(blueprint, config, stack, 1L);
        stack.setCluster(cluster);
        return cluster;
    }

    private void mockAll() throws IOException {
        when(ambariViewProvider.provideViewInformation(any(AmbariClient.class), any(Cluster.class))).thenAnswer(new Answer<Cluster>() {
            @Override
            public Cluster answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return (Cluster) args[1];
            }
        });
        given(ambariViewProvider.isViewDefinitionNotProvided(any(Cluster.class))).willReturn(false);
        given(blueprintValidator.getHostGroupNode(any(Blueprint.class))).willReturn(jsonNode);
        given(jsonNode.iterator()).willReturn(mockIterator);
        given(mockIterator.hasNext()).willReturn(true).willReturn(false);
        given(mockIterator.next()).willReturn(jsonNode);
        given(conversionService.convert(any(RDSConfig.class), eq(RDSConfigJson.class))).willReturn(new RDSConfigRequest());
        given(blueprintValidator.getHostGroupName(jsonNode)).willReturn("slave_1");
        given(blueprintValidator.createHostGroupMap(any(Set.class))).willReturn(hostGroupMap);
        given(hostGroupMap.get("slave_1")).willReturn(hostGroup);
        given(instanceGroup.getInstanceMetaData()).willReturn(Sets.newHashSet(instanceMetaData));
        given(blueprintValidator.getComponentsNode(jsonNode)).willReturn(nameJsonNode);
        given(nameJsonNode.iterator()).willReturn(mockComponentIterator);
        given(mockComponentIterator.hasNext()).willReturn(true).willReturn(false);
        given(mockComponentIterator.next()).willReturn(nameJsonNode);
        given(nameJsonNode.get(anyString())).willReturn(nameJsonNode);
        given(nameJsonNode.asText()).willReturn("dummyName");
    }

    private StackServiceComponentDescriptor createStackServiceComponentDescriptor() {
        return new StackServiceComponentDescriptor("ELASTIC_SEARCH", "MASTER", 1, 1);
    }
}
