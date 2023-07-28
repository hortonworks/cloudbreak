package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.restart.ClusterServicesRestartRequest;
import com.sequenceiq.cloudbreak.service.ClusterServicesRestartService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;

public class CusterServicesRestartHandlerTest {

    @Mock
    private ClusterApiConnectors apiConnectors;

    @Mock
    private StackService stackService;

    @Mock
    private EventBus eventBus;

    @Mock
    private DatalakeService datalakeService;

    @Mock
    private ClusterApi connector;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private ClusterServicesRestartService clusterServicesRestartService;

    @Mock
    private ClusterModificationService clusterModificationService;

    @InjectMocks
    private ClusterServicesRestartHandler underTest;

    private Stack stack;

    @BeforeEach
    void setUp() {
        underTest = new ClusterServicesRestartHandler();
        MockitoAnnotations.openMocks(this);
        mockTemplateComponents();

        stack = new Stack();
        stack.setId(1L);
        stack.setCloudPlatform("AWS");
        Cluster cluster = new Cluster();
        cluster.setBlueprint(new Blueprint());
        stack.setCluster(cluster);

        when(datalakeService.getDatalakeStackByDatahubStack(any())).thenReturn(Optional.of(stack));
        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);

    }

    @Test
    public void testRefreshNeeded() throws Exception {
        when(clusterServicesRestartService.isRemoteDataContextRefreshNeeded(any(), any())).thenReturn(true);
        underTest.accept(new Event(new ClusterServicesRestartRequest(stack.getId())));

        verify(clusterServicesRestartService).refreshClusterOnRestart(any(), any(), any());
        verifyNoInteractions(apiConnectors);
    }

    @Test
    public void testRefreshNotNeeded() throws Exception {
        when(apiConnectors.getConnector(any(Stack.class))).thenReturn(connector);
        when(connector.clusterModificationService()).thenReturn(clusterModificationService);
        when(clusterServicesRestartService.isRemoteDataContextRefreshNeeded(any(), any())).thenReturn(false);
        underTest.accept(new Event(new ClusterServicesRestartRequest(stack.getId())));

        verify(clusterServicesRestartService, times(0)).refreshClusterOnRestart(any(), any(), any());
        verify(clusterModificationService).restartClusterServices();

    }

    private void mockTemplateComponents() {
        CmTemplateProcessor cmTemplateProcessor = mock(CmTemplateProcessor.class);
        when(cmTemplateProcessorFactory.get(any())).thenReturn(cmTemplateProcessor);
        lenient().when(cmTemplateProcessor.getAllComponents()).thenReturn(new HashSet<>(Arrays.asList(
                ServiceComponent.of("HBASE", "MASTER"),
                ServiceComponent.of("HBASE", "REGIONSERVER"),
                ServiceComponent.of("HBASE", "HBASERESTSERVER"),
                ServiceComponent.of("PHOENIX", "PHOENIX_QUERY_SERVER"),
                ServiceComponent.of("CLOUDERA_MANAGER", "CM-API"),
                ServiceComponent.of("CLOUDERA_MANAGER_UI", "CM-UI")
        )));
    }

}
