package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.Node;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.connector.MetadataSetup;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

@RunWith(MockitoJUnitRunner.class)
public class ClusterBootstrapperErrorHandlerTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Mock
    private InstanceGroupRepository instanceGroupRepository;

    @Mock
    private HostMetadataRepository hostMetadataRepository;

    @Mock
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @Mock
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @Mock
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private Map<CloudPlatform, MetadataSetup> metadataSetups;

    @InjectMocks
    private ClusterBootstrapperErrorHandler underTest;

    @Test
    public void test() throws CloudbreakOrchestratorFailedException {

        doNothing().when(eventService).fireCloudbreakEvent(anyLong(), anyString(), anyString());
        when(instanceGroupRepository.save(any(InstanceGroup.class))).thenReturn(new InstanceGroup());
        when(instanceMetaDataRepository.save(any(InstanceMetaData.class))).thenReturn(new InstanceMetaData());
        when(instanceMetaDataRepository.findNotTerminatedByPrivateAddress(anyLong(), anyString())).thenReturn(new InstanceMetaData());
        when(instanceGroupRepository.findOneByGroupNameInStack(anyLong(), anyString())).thenReturn(new InstanceGroup());

        underTest.terminateFailedNodes(new MockContainerOrchestrator(), TestUtil.stack(), new GatewayConfig("10.0.0.1", "/cert/1"), new HashSet<Node>());
    }

}