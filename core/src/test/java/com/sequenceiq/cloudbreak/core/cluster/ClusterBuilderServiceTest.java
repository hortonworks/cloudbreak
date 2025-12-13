package com.sequenceiq.cloudbreak.core.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
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
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
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

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @BeforeEach
    void setUp() {
        lenient().when(mockBlueprint.getBlueprintJsonText()).thenReturn(BLUEPRINT_TEXT);
        lenient().when(mockCluster.getExtendedBlueprintText()).thenReturn(BLUEPRINT_TEXT);
        lenient().when(mockCluster.getId()).thenReturn(CLUSTER_ID);
        lenient().when(mockCluster.getProxyConfigCrn()).thenReturn(PROXY_CRN);
        lenient().when(mockCluster.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(mockStack.getBlueprint()).thenReturn(mockBlueprint);
        lenient().when(mockStack.getBlueprintJsonText()).thenReturn(BLUEPRINT_TEXT);
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
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        underTest.prepareExtendedTemplate(STACK_ID);

        verify(mockClusterService, times(2)).updateExtendedBlueprintText(any(), anyString());
        verify(mockClusterService, times(2)).updateExtendedBlueprintText(CLUSTER_ID, BLUEPRINT_TEXT);
    }

    @Test
    void testPrepareExtendedTemplateWhenBlueprintTextContainUnfilledHandlebarPropertiesThenItShouldThrowException() {
        String firstHandlebarProperty = "stuFF.value";
        String secondHandlebarProperty = "stuFF2.value";
        String blueprintWithHandlebars = String.format("{\"some\":\"thing\",\"other\":\"{{{ %s }}}\",\"some2\":\"thingie\",\"rehto\":\"{{{ %s }}}\"}",
                firstHandlebarProperty, secondHandlebarProperty);

        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(mockClusterSetupService.prepareTemplate(any(), any(), any(), any(), any())).thenReturn(blueprintWithHandlebars);

        IllegalStateException expectedException = assertThrows(IllegalStateException.class, () ->
                underTest.prepareExtendedTemplate(STACK_ID));

        assertEquals(String.format("Some of the template parameters has not been resolved! Please check your custom properties at cluster the " +
                "cluster creation to be able to resolve them! Remaining handlebar value: {{{ %s }}}", firstHandlebarProperty), expectedException.getMessage());

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

    @Test
    void configureManagementServiceCDLDatalake() {
        when(proxyConfigDtoService.getByCrnWithEnvironmentFallback(PROXY_CRN, ENV_CRN)).thenReturn(Optional.of(proxyConfig));
        when(mockStackDtoService.getById(eq(STACK_ID))).thenReturn(mockStack);
        when(mockStack.getStack()).thenReturn(stackView);
        when(mockStack.getDatalakeCrn()).thenReturn(null);
        when(mockStack.getEnvironmentCrn()).thenReturn("envcrn");
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        underTest.configureManagementServices(STACK_ID);
        verify(platformAwareSdxConnector, times(1)).getSdxBasicViewByEnvironmentCrn(anyString());
    }

    @Test
    void configureManagementServicePaaSDatalake() {
        when(proxyConfigDtoService.getByCrnWithEnvironmentFallback(PROXY_CRN, ENV_CRN)).thenReturn(Optional.of(proxyConfig));
        when(mockStackDtoService.getById(eq(STACK_ID))).thenReturn(mockStack);
        when(mockStack.getStack()).thenReturn(stackView);
        when(mockStack.getDatalakeCrn()).thenReturn("dlCrn");
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        underTest.configureManagementServices(STACK_ID);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(platformAwareSdxConnector, times(1)).getRemoteDataContext(any());
        verify(platformAwareSdxConnector, times(0)).getSdxBasicViewByEnvironmentCrn(anyString());
        verify(mockClusterSetupService, times(1)).configureManagementServices(any(), any(), captor.capture(), any(), any());
        assertEquals("dlCrn", captor.getValue());
    }

    @Test
    void configureManagementServiceWorkloadPaaSDatalake() {
        when(proxyConfigDtoService.getByCrnWithEnvironmentFallback(PROXY_CRN, ENV_CRN)).thenReturn(Optional.of(proxyConfig));
        when(mockStackDtoService.getById(eq(STACK_ID))).thenReturn(mockStack);
        when(mockStack.getStack()).thenReturn(stackView);
        when(mockStack.getDatalakeCrn()).thenReturn("dlCrn");
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        underTest.configureManagementServices(STACK_ID);
        verify(platformAwareSdxConnector, times(1)).getRemoteDataContext(any());
        verify(platformAwareSdxConnector, times(0)).getSdxBasicViewByEnvironmentCrn(anyString());
    }

    @Test
    void configureManagementServiceWorkloadPaaSDatalakeNoCRN() {
        when(proxyConfigDtoService.getByCrnWithEnvironmentFallback(PROXY_CRN, ENV_CRN)).thenReturn(Optional.of(proxyConfig));
        when(mockStackDtoService.getById(eq(STACK_ID))).thenReturn(mockStack);
        when(mockStack.getStack()).thenReturn(stackView);
        when(mockStack.getDatalakeCrn()).thenReturn(null);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        underTest.configureManagementServices(STACK_ID);
        verify(platformAwareSdxConnector, times(0)).getRemoteDataContext(any());
        verify(platformAwareSdxConnector, times(0)).getSdxBasicViewByEnvironmentCrn(anyString());
    }

}