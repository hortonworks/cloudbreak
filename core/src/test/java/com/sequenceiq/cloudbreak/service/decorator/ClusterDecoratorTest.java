package com.sequenceiq.cloudbreak.service.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.FileReaderUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateValidator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintValidatorFactory;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class ClusterDecoratorTest {

    @InjectMocks
    private ClusterDecorator underTest;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private BlueprintValidatorFactory blueprintValidatorFactory;

    @Mock
    private CmTemplateValidator cmTemplateValidator;

    @Mock
    private StackService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Mock
    private Stack stack;

    @Mock
    private User user;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector<Object> connector;

    @Mock
    private PlatformParameters platformParameters;

    @BeforeEach
    void setUp() {
        when(blueprintValidatorFactory.createBlueprintValidator(any())).thenReturn(cmTemplateValidator);
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(connector);
        when(connector.parameters()).thenReturn(platformParameters);
    }

    @Test
    void testDecorateIfMethodCalledThenSharedServiceConfigProviderShouldBeCalledOnceToConfigureTheCluster() {
        Cluster expectedClusterInstance = new Cluster();
        Blueprint blueprint = getBlueprint();
        when(sharedServiceConfigProvider.configureCluster(any(Cluster.class), any(User.class), any(Workspace.class)))
                .thenReturn(expectedClusterInstance);
        Cluster result = underTest.decorate(expectedClusterInstance, createClusterV4Request(), blueprint, user, new Workspace(), stack, null);

        assertEquals(expectedClusterInstance, result);
        verify(sharedServiceConfigProvider, times(1)).configureCluster(any(Cluster.class), any(User.class), any(Workspace.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    void testAutoTlsSetting(String valueString) {
        boolean useAutoTls = Boolean.parseBoolean(valueString);
        Cluster expectedClusterInstance = new Cluster();
        Blueprint blueprint = getBlueprint();
        when(sharedServiceConfigProvider.configureCluster(any(Cluster.class), any(User.class), any(Workspace.class)))
                .thenReturn(expectedClusterInstance);
        when(platformParameters.isAutoTlsSupported()).thenReturn(useAutoTls);

        Cluster result = underTest.decorate(expectedClusterInstance, createClusterV4Request(), blueprint, user, new Workspace(), stack, null);

        assertEquals(useAutoTls, result.getAutoTlsEnabled());
    }

    @Test
    void testAutoTlsSettingByParentEnvironmentCloudPlatform() {
        Cluster expectedClusterInstance = new Cluster();
        Blueprint blueprint = getBlueprint();
        when(sharedServiceConfigProvider.configureCluster(any(Cluster.class), any(User.class), any(Workspace.class)))
                .thenReturn(expectedClusterInstance);
        ArgumentCaptor<Platform> platformArgumentCaptor = ArgumentCaptor.forClass(Platform.class);
        when(cloudPlatformConnectors.get(platformArgumentCaptor.capture(), any())).thenReturn(connector);

        String platform = CloudPlatform.YARN.name();
        underTest.decorate(expectedClusterInstance, createClusterV4Request(), blueprint, user, new Workspace(), stack, platform);

        assertEquals(platform, platformArgumentCaptor.getValue().value());
    }

    private Blueprint getBlueprint() {
        Blueprint blueprint = new Blueprint();
        String blueprintText = FileReaderUtil.readResourceFile(this, "ha-components.bp");
        blueprint.setBlueprintText(blueprintText);
        return blueprint;
    }

    private ClusterV4Request createClusterV4Request() {
        return new ClusterV4Request();
    }
}
