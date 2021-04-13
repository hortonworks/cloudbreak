package com.sequenceiq.cloudbreak.core.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

class ClusterBuilderServiceTest {

    private static final Long STACK_ID = 1L;

    private static final Long CLUSTER_ID = 1L;

    private static final Long INSTANCE_GROUP_ID = 1L;

    private static final String BLUEPRINT_TEXT = "{\"some\":\"thing\"}";

    @InjectMocks
    private ClusterBuilderService underTest;

    @Mock
    private HostGroupService mockHostGroupService;

    @Mock
    private ClusterService mockClusterService;

    @Mock
    private StackService mockStackService;

    @Mock
    private ConversionService mockConversionService;

    @Mock
    private DatalakeResourcesService mockDatalakeResourcesService;

    @Mock
    private KerberosConfigService mockKerberosConfigService;

    @Mock
    private ClusterApiConnectors mockClusterApiConnectors;

    @Mock
    private ClusterApi mockClusterApi;

    @Mock
    private ClusterSetupService mockClusterSetupService;

    @Mock
    private InstanceMetaDataService mockInstanceMetaDataService;

    @Mock
    private Blueprint mockBlueprint;

    @Mock
    private Workspace mockWorkspace;

    @Mock
    private Cluster mockCluster;

    @Mock
    private Stack mockStack;

    @Mock
    private HostGroup mockHostGroup;

    @Mock
    private InstanceGroup mockInstanceGroup;

    @Mock
    private InstanceMetaData mockInstanceMetaData;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockBlueprint.getBlueprintText()).thenReturn(BLUEPRINT_TEXT);
        when(mockCluster.getId()).thenReturn(CLUSTER_ID);
        when(mockCluster.getBlueprint()).thenReturn(mockBlueprint);
        when(mockStack.getCluster()).thenReturn(mockCluster);
        when(mockStack.getWorkspace()).thenReturn(mockWorkspace);
        when(mockInstanceGroup.getId()).thenReturn(INSTANCE_GROUP_ID);
        when(mockHostGroup.getInstanceGroup()).thenReturn(mockInstanceGroup);

        when(mockStackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(mockStack);
        when(mockClusterApiConnectors.getConnector(mockStack)).thenReturn(mockClusterApi);
        when(mockClusterApi.clusterSetupService()).thenReturn(mockClusterSetupService);
        when(mockHostGroupService.getByClusterWithRecipes(CLUSTER_ID)).thenReturn(Set.of(mockHostGroup));
        when(mockInstanceMetaDataService.findAliveInstancesInInstanceGroup(INSTANCE_GROUP_ID)).thenReturn(List.of(mockInstanceMetaData));
    }

    @Test
    void testPrepareExtendedTemplateWhenBlueprintTextDoesNotContainUnfilledHandlebarPropertiesThenItShouldNotThrowException() {
        when(mockClusterSetupService.prepareTemplate(any(), any(), any(), any(), any())).thenReturn(BLUEPRINT_TEXT);
        underTest.prepareExtendedTemplate(STACK_ID);

        verify(mockCluster, times(2)).setExtendedBlueprintText(anyString());
        verify(mockCluster, times(2)).setExtendedBlueprintText(BLUEPRINT_TEXT);
    }

    @Test
    void testPrepareExtendedTemplateWhenBlueprintTextContainUnfilledHandlebarPropertiesThenItShouldThrowException() {
        String firstHandlebarProperty = "stuff.value";
        String secondHandlebarProperty = "stuff2.value";
        String blueprintWithHandlebars = String.format("{\"some\":\"thing\",\"other\":\"{{{ %s }}}\",\"some2\":\"thingie\",\"rehto\":\"{{{ %s }}}\"}",
                firstHandlebarProperty, secondHandlebarProperty);

        when(mockClusterSetupService.prepareTemplate(any(), any(), any(), any(), any())).thenReturn(blueprintWithHandlebars);

        IllegalStateException expectedException = Assertions.assertThrows(IllegalStateException.class, () ->
                underTest.prepareExtendedTemplate(STACK_ID));

        assertEquals("Some of the template parameters has not been resolved! Please check your custom properties at cluster the " +
                        "cluster creation to be able to resolve them!", expectedException.getMessage());

        verify(mockCluster, times(1)).setExtendedBlueprintText(anyString());
    }

}