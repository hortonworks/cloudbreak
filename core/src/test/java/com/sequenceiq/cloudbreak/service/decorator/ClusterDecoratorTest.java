package com.sequenceiq.cloudbreak.service.decorator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.FileReaderUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.blueprint.validation.AmbariBlueprintValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.AmbariHaComponentFilter;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public class ClusterDecoratorTest {

    @InjectMocks
    private ClusterDecorator underTest;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private AmbariBlueprintValidator ambariBlueprintValidator;

    @Mock
    private StackService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private RdsConnectionValidator rdsConnectionValidator;

    @Mock
    private ClusterProxyDecorator clusterProxyDecorator;

    @Mock
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Mock
    private Stack stack;

    @Mock
    private AmbariHaComponentFilter ambariHaComponentFilter;

    @Mock
    private User user;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDecorateIfMethodCalledThenSharedServiceConfigProviderShouldBeCalledOnceToConfigureTheCluster() {
        Cluster expectedClusterInstance = new Cluster();
        Blueprint blueprint = new Blueprint();
        String blueprintText = FileReaderUtil.readResourceFile(this, "ha-components.bp");
        blueprint.setBlueprintText(blueprintText);
        when(sharedServiceConfigProvider.configureCluster(any(Cluster.class), any(User.class), any(Workspace.class)))
                .thenReturn(expectedClusterInstance);
        when(clusterProxyDecorator.prepareProxyConfig(any(Cluster.class), any())).thenReturn(expectedClusterInstance);
        when(ambariHaComponentFilter.getHaComponents(any())).thenReturn(Collections.emptySet());
        Cluster result = underTest.decorate(expectedClusterInstance, createClusterV4Request(), blueprint, user, new Workspace(), stack);

        Assert.assertEquals(expectedClusterInstance, result);
        verify(sharedServiceConfigProvider, times(1)).configureCluster(any(Cluster.class), any(User.class), any(Workspace.class));
    }

    private ClusterV4Request createClusterV4Request() {
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        AmbariV4Request ambariV4Request = new AmbariV4Request();
        clusterV4Request.setAmbari(ambariV4Request);
        return clusterV4Request;
    }
}
