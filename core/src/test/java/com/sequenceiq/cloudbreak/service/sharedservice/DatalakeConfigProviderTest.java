package com.sequenceiq.cloudbreak.service.sharedservice;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.clusterdefinition.CentralBlueprintParameterQueryService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptor;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptorDefinition;
import com.sequenceiq.cloudbreak.repository.cluster.DatalakeResourcesRepository;
import com.sequenceiq.cloudbreak.repository.cluster.ServiceDescriptorRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariClientFactory;

@RunWith(MockitoJUnitRunner.class)
public class DatalakeConfigProviderTest {
    @Mock
    private AmbariClientFactory ambariClientFactory;

    @Mock
    private ServiceDescriptorDefinitionProvider serviceDescriptorDefinitionProvider;

    @Mock
    private CentralBlueprintParameterQueryService centralBlueprintParameterQueryService;

    @Mock
    private DatalakeResourcesRepository datalakeResourcesRepository;

    @Mock
    private ServiceDescriptorRepository serviceDescriptorRepository;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private DatalakeConfigProvider underTest;

    @Test
    public void test() throws Exception {
        // GIVEN
        AmbariClient ambariClient = mock(AmbariClient.class);
        Map<String, Map<String, String>> serviceSecretParamMap = Map.ofEntries(Map.entry("service1",
                Map.ofEntries(Map.entry("blueprintsecretparam1", "blueprintsecretparam1"), Map.entry("blueprintsecretparam2", "blueprintsecretparam2"))),
                Map.entry("service2", Map.ofEntries(Map.entry("blueprintsecretparam3", "blueprintsecretparam3"),
                        Map.entry("blueprintsecretparam4", "blueprintsecretparam4"))));
        ServiceDescriptorDefinition serviceDescriptorDefinition1 = new ServiceDescriptorDefinition();
        serviceDescriptorDefinition1.setServiceName("service1");
        serviceDescriptorDefinition1.setBlueprintParamKeys(Set.of("blueprintparam1", "blueprintparam2"));
        serviceDescriptorDefinition1.setBlueprintSecretParamKeys(Set.of("blueprintsecretparam1", "blueprintsecretparam2"));
        serviceDescriptorDefinition1.setComponentHosts(Set.of("component1", "component2"));
        ServiceDescriptorDefinition serviceDescriptorDefinition2 = new ServiceDescriptorDefinition();
        serviceDescriptorDefinition2.setServiceName("service2");
        serviceDescriptorDefinition2.setBlueprintParamKeys(Set.of("blueprintparam3", "blueprintparam4"));
        serviceDescriptorDefinition2.setBlueprintSecretParamKeys(Set.of("blueprintsecretparam3", "blueprintsecretparam4"));
        serviceDescriptorDefinition2.setComponentHosts(Set.of("component3", "component4"));
        Map<String, String> ambariParameters = Map.ofEntries(
                Map.entry("blueprintparam1", "blueprintparam1"),
                Map.entry("blueprintparam2", "blueprintparam2"),
                Map.entry("blueprintparam3", "blueprintparam3"),
                Map.entry("blueprintparam4", "blueprintparam4"),
                Map.entry("blueprintsecretparam1", "blueprintsecretparam1"),
                Map.entry("blueprintsecretparam2", "blueprintsecretparam2"),
                Map.entry("blueprintsecretparam3", "blueprintsecretparam3"),
                Map.entry("blueprintsecretparam4", "blueprintsecretparam4"));
        Set<String> configIds = new HashSet<>();
        configIds.addAll(serviceDescriptorDefinition1.getBlueprintParamKeys());
        configIds.addAll(serviceDescriptorDefinition2.getBlueprintParamKeys());
        when(serviceDescriptorDefinitionProvider.getServiceDescriptorDefinitionMap()).thenReturn(Map.ofEntries(
                Map.entry(serviceDescriptorDefinition1.getServiceName(), serviceDescriptorDefinition1),
                Map.entry(serviceDescriptorDefinition2.getServiceName(), serviceDescriptorDefinition2)));
        when(ambariClient.getConfigValuesByConfigIds(Lists.newArrayList(configIds))).thenReturn(ambariParameters);
        when(ambariClient.getHostNamesByComponent("component1")).thenReturn(List.of("host1"));
        when(ambariClient.getHostNamesByComponent("component2")).thenReturn(List.of("host1", "host2"));
        when(ambariClient.getHostNamesByComponent("component3")).thenReturn(List.of("host2"));
        when(ambariClient.getHostNamesByComponent("component4")).thenReturn(List.of("host3"));
        // WHEN
        DatalakeResources datalakeResources = underTest.collectDatalakeResources("ambariName", "ambariUrl", "ambariIp", "ambariFqdn", ambariClient,
                serviceSecretParamMap, null, null, null);
        // THEN
        Assert.assertNotNull(datalakeResources);
        ServiceDescriptor serviceDescriptor1 = datalakeResources.getServiceDescriptorMap().get("service1");
        Assert.assertEquals(serviceDescriptor1.getBlueprintParams().getMap().size(), serviceDescriptorDefinition1.getBlueprintParamKeys().size());
        for (Map.Entry blueprintParam : serviceDescriptor1.getBlueprintParams().getMap().entrySet()) {
            Assert.assertTrue(serviceDescriptorDefinition1.getBlueprintParamKeys().contains(blueprintParam.getKey()));
            Assert.assertEquals(blueprintParam.getValue(), ambariParameters.get(blueprintParam.getKey()));
        }
        Assert.assertEquals(serviceDescriptor1.getBlueprintSecretParams().getMap().size(), serviceDescriptorDefinition1.getBlueprintSecretParamKeys().size());
        for (Map.Entry blueprintSecretParam : serviceDescriptor1.getBlueprintSecretParams().getMap().entrySet()) {
            Assert.assertTrue(serviceDescriptorDefinition1.getBlueprintSecretParamKeys().contains(blueprintSecretParam.getKey()));
            Assert.assertEquals(blueprintSecretParam.getValue(), ambariParameters.get(blueprintSecretParam.getKey()));
        }
        ServiceDescriptor serviceDescriptor2 = datalakeResources.getServiceDescriptorMap().get("service2");
        Assert.assertEquals(serviceDescriptor2.getBlueprintParams().getMap().size(), serviceDescriptorDefinition2.getBlueprintParamKeys().size());
        for (Map.Entry blueprintParam : serviceDescriptor2.getBlueprintParams().getMap().entrySet()) {
            Assert.assertTrue(serviceDescriptorDefinition2.getBlueprintParamKeys().contains(blueprintParam.getKey()));
            Assert.assertEquals(blueprintParam.getValue(), ambariParameters.get(blueprintParam.getKey()));
        }
        Assert.assertEquals(serviceDescriptor2.getBlueprintSecretParams().getMap().size(), serviceDescriptorDefinition2.getBlueprintSecretParamKeys().size());
        for (Map.Entry blueprintSecretParam : serviceDescriptor2.getBlueprintSecretParams().getMap().entrySet()) {
            Assert.assertTrue(serviceDescriptorDefinition2.getBlueprintSecretParamKeys().contains(blueprintSecretParam.getKey()));
            Assert.assertEquals(blueprintSecretParam.getValue(), ambariParameters.get(blueprintSecretParam.getKey()));
        }
    }
}
