package com.sequenceiq.cloudbreak.core.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.saas.sdx.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith({MockitoExtension.class})
class ClusterBuilderServiceTest {

    private static final Long STACK_ID = 1L;

    private static final Long CLUSTER_ID = 1L;

    private static final Long INSTANCE_GROUP_ID = 1L;

    private static final String BLUEPRINT_TEXT = "{\"some\":\"thing\"}";

    private static final String PROXY_CRN = "proxy-crn";

    private static final String ENV_CRN = "env-crn";

    @InjectMocks
    private ClusterBuilderService underTest;

    @Mock
    private HostGroupService mockHostGroupService;

    @Mock
    private ClusterService mockClusterService;

    @Mock
    private StackDtoService mockStackDtoService;

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
    private ClusterView mockCluster;

    @Mock
    private StackView stackView;

    @Mock
    private StackDto mockStack;

    @Mock
    private HostGroup mockHostGroup;

    @Mock
    private InstanceGroup mockInstanceGroup;

    @Mock
    private InstanceMetaData mockInstanceMetaData;

    @Mock
    private StackToTemplatePreparationObjectConverter stackToTemplatePreparationObjectConverter;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private ProxyConfig proxyConfig;

    @Mock
    private ProxyConfigDtoService proxyConfigDtoService;

    @BeforeEach
    void setUp() {
        lenient().when(mockBlueprint.getBlueprintText()).thenReturn(BLUEPRINT_TEXT);
        lenient().when(mockCluster.getId()).thenReturn(CLUSTER_ID);
        lenient().when(mockCluster.getProxyConfigCrn()).thenReturn(PROXY_CRN);
        lenient().when(mockCluster.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(mockStack.getBlueprint()).thenReturn(mockBlueprint);
        lenient().when(mockStack.getCluster()).thenReturn(mockCluster);
        lenient().when(mockStack.getStack()).thenReturn(stackView);
        lenient().when(mockInstanceGroup.getId()).thenReturn(INSTANCE_GROUP_ID);
        lenient().when(mockHostGroup.getInstanceGroup()).thenReturn(mockInstanceGroup);

        lenient().when(mockStackDtoService.getById(STACK_ID)).thenReturn(mockStack);
        lenient().when(mockClusterApiConnectors.getConnector(mockStack)).thenReturn(mockClusterApi);
        lenient().when(mockClusterApi.clusterSetupService()).thenReturn(mockClusterSetupService);
        lenient().when(mockHostGroupService.getByClusterWithRecipes(CLUSTER_ID)).thenReturn(Set.of(mockHostGroup));
        lenient().when(mockInstanceMetaDataService.findAliveInstancesInInstanceGroup(INSTANCE_GROUP_ID)).thenReturn(List.of(mockInstanceMetaData));
    }

    @Test
    void testPrepareExtendedTemplateWhenBlueprintTextDoesNotContainUnfilledHandlebarPropertiesThenItShouldNotThrowException() {
        when(mockClusterSetupService.prepareTemplate(any(), any(), any(), any(), any())).thenReturn(BLUEPRINT_TEXT);
        when(stackToTemplatePreparationObjectConverter.convert(any()))
                .thenReturn(new TemplatePreparationObject.Builder().build());
        underTest.prepareExtendedTemplate(STACK_ID);

        verify(mockClusterService, times(2)).updateExtendedBlueprintText(any(), anyString());
        verify(mockClusterService, times(2)).updateExtendedBlueprintText(CLUSTER_ID, BLUEPRINT_TEXT);
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

        verify(mockClusterService, times(1)).updateExtendedBlueprintText(any(), anyString());
    }

    @Test
    void prepareProxyConfigForStackWithoutProxy() {
        when(proxyConfigDtoService.getByCrnWithEnvironmentFallback(PROXY_CRN, ENV_CRN)).thenReturn(Optional.empty());

        underTest.prepareProxyConfig(STACK_ID);

        verifyNoInteractions(mockClusterSetupService);
    }

    @Test
    void prepareProxyConfigForStackWithProxy() {
        when(proxyConfigDtoService.getByCrnWithEnvironmentFallback(PROXY_CRN, ENV_CRN)).thenReturn(Optional.of(proxyConfig));

        underTest.prepareProxyConfig(STACK_ID);

        verify(mockClusterSetupService).setupProxy(proxyConfig);
    }

    @Test
    void modifyProxyConfigForStackWithoutProxy() {
        when(proxyConfigDtoService.getByCrnWithEnvironmentFallback(PROXY_CRN, ENV_CRN)).thenReturn(Optional.empty());

        underTest.modifyProxyConfig(STACK_ID);

        verify(mockClusterSetupService).setupProxy(null);
    }

    @Test
    void modifyProxyConfigForStackWithProxy() {
        when(proxyConfigDtoService.getByCrnWithEnvironmentFallback(PROXY_CRN, ENV_CRN)).thenReturn(Optional.of(proxyConfig));

        underTest.modifyProxyConfig(STACK_ID);

        verify(mockClusterSetupService).setupProxy(proxyConfig);
    }

}