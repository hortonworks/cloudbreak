package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import static java.util.Collections.emptySet;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostMetadataResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;

@RunWith(MockitoJUnitRunner.class)
public class StackResponseHardwareInfoProviderTest {

    @InjectMocks
    private final StackResponseHardwareInfoProvider underTest = new StackResponseHardwareInfoProvider();

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @Mock
    private ConversionService conversionService;

    @Test
    public void testProviderEntriesToStackResponseEmptyInstanceGroup() {

        Stack stack = new Stack();
        stack.setInstanceGroups(emptySet());

        StackResponse actual = underTest.providerEntriesToStackResponse(stack, new StackResponse());

        Assert.assertEquals(0, actual.getHardwareInfos().size());
    }

    @Test
    public void testProviderEntriesToStackResponseEmptyInstanceMetadata() {

        Stack stack = new Stack();
        stack.setInstanceGroups(Sets.newHashSet(new InstanceGroup()));

        StackResponse actual = underTest.providerEntriesToStackResponse(stack, new StackResponse());

        Assert.assertEquals(0, actual.getHardwareInfos().size());
    }

    @Test
    public void testProviderEntriesToStackResponseClusterNull() {

        Stack stack = new Stack();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        stack.setInstanceGroups(getInstanceGroups(instanceMetaData));

        StackResponse actual = underTest.providerEntriesToStackResponse(stack, new StackResponse());

        Assert.assertEquals(1, actual.getHardwareInfos().size());

        Mockito.verify(hostMetadataRepository, Mockito.times(0)).findHostInClusterByName(anyLong(), anyString());
        Mockito.verify(conversionService, Mockito.times(1)).convert(instanceMetaData, InstanceMetaDataJson.class);
        Mockito.verify(conversionService, Mockito.times(1)).convert(null, HostMetadataResponse.class);
    }

    @Test
    public void testProviderEntriesToStackResponseClusterNotNullButFQDNNull() {

        Stack stack = new Stack();
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        stack.setInstanceGroups(getInstanceGroups(instanceMetaData));

        StackResponse actual = underTest.providerEntriesToStackResponse(stack, new StackResponse());

        Assert.assertEquals(1, actual.getHardwareInfos().size());

        Mockito.verify(hostMetadataRepository, Mockito.times(0)).findHostInClusterByName(anyLong(), anyString());
        Mockito.verify(conversionService, Mockito.times(1)).convert(instanceMetaData, InstanceMetaDataJson.class);
        Mockito.verify(conversionService, Mockito.times(1)).convert(null, HostMetadataResponse.class);
    }

    @Test
    public void testProviderEntriesToStackResponseClusterNotNullAndFQDNNotNull() {

        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("fqdn");
        stack.setInstanceGroups(getInstanceGroups(instanceMetaData));

        StackResponse stackResponse = new StackResponse();
        StackResponse actual = underTest.providerEntriesToStackResponse(stack, stackResponse);

        Assert.assertEquals(1, actual.getHardwareInfos().size());

        Mockito.verify(hostMetadataRepository, Mockito.times(1)).findHostInClusterByName(1L, "fqdn");
        Mockito.verify(conversionService, Mockito.times(1)).convert(instanceMetaData, InstanceMetaDataJson.class);
        Mockito.verify(conversionService, Mockito.times(1)).convert(null, HostMetadataResponse.class);
    }

    @Test
    public void testProviderEntriesToStackResponseMultipleInstanceGroup() {

        Stack stack = new Stack();
        stack.setCluster(new Cluster());
        Set<InstanceGroup> instanceGroups = getInstanceGroups(new InstanceMetaData());
        instanceGroups.addAll(getInstanceGroups(new InstanceMetaData()));
        stack.setInstanceGroups(instanceGroups);

        StackResponse actual = underTest.providerEntriesToStackResponse(stack, new StackResponse());

        Assert.assertEquals(2, actual.getHardwareInfos().size());

        Mockito.verify(hostMetadataRepository, Mockito.times(0)).findHostInClusterByName(anyLong(), anyString());
        Mockito.verify(conversionService, Mockito.times(2)).convert(any(InstanceMetaData.class), eq(InstanceMetaDataJson.class));
        Mockito.verify(conversionService, Mockito.times(2)).convert(null, HostMetadataResponse.class);
    }

    @Test
    public void testProviderEntriesToStackResponseConvertsResult() {

        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        stack.setCluster(cluster);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("fqdn");
        stack.setInstanceGroups(getInstanceGroups(instanceMetaData));

        InstanceMetaDataJson instanceMetaDataJson = new InstanceMetaDataJson();
        HostMetadataResponse hostMetadataResponse = new HostMetadataResponse();

        HostMetadata hostMetadata = new HostMetadata();

        Mockito.when(hostMetadataRepository.findHostInClusterByName(1L, "fqdn")).thenReturn(hostMetadata);
        Mockito.when(conversionService.convert(instanceMetaData, InstanceMetaDataJson.class)).thenReturn(instanceMetaDataJson);
        Mockito.when(conversionService.convert(hostMetadata, HostMetadataResponse.class)).thenReturn(hostMetadataResponse);

        StackResponse actual = underTest.providerEntriesToStackResponse(stack, new StackResponse());

        Assert.assertEquals(1, actual.getHardwareInfos().size());

        Mockito.verify(hostMetadataRepository, Mockito.times(1)).findHostInClusterByName(1L, "fqdn");
        Mockito.verify(conversionService, Mockito.times(1)).convert(instanceMetaData, InstanceMetaDataJson.class);
        Mockito.verify(conversionService, Mockito.times(1)).convert(hostMetadata, HostMetadataResponse.class);


    }

    private Set<InstanceGroup> getInstanceGroups(InstanceMetaData... instanceMetaData) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setInstanceMetaData(Sets.newHashSet(instanceMetaData));
        return Sets.newHashSet(instanceGroup);
    }
}
