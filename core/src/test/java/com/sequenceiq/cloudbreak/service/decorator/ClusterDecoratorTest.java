package com.sequenceiq.cloudbreak.service.decorator;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
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
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.FileReaderUtil;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.converter.mapper.AmbariDatabaseMapper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.AmbariHaComponentFilter;
import com.sequenceiq.cloudbreak.service.blueprint.LegacyBlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.ldapconfig.DefaultLdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.DefaultRdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;

public class ClusterDecoratorTest {

    @InjectMocks
    private ClusterDecorator underTest;

    @Mock
    private LegacyBlueprintService blueprintService;

    @Mock
    private BlueprintValidator blueprintValidator;

    @Mock
    private StackService stackService;

    @Mock
    private ConversionService conversionService;

    @Mock
    private HostGroupDecorator hostGroupDecorator;

    @Mock
    private DefaultRdsConfigService rdsConfigService;

    @Mock
    private DefaultLdapConfigService ldapConfigService;

    @Mock
    private LdapConfigValidator ldapConfigValidator;

    @Mock
    private ClusterService clusterService;

    @Mock
    private RdsConnectionValidator rdsConnectionValidator;

    @Mock
    private ClusterProxyDecorator clusterProxyDecorator;

    @Mock
    private AmbariDatabaseMapper ambariDatabaseMapper;

    @Mock
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Mock
    private ClusterRequest request;

    @Mock
    private IdentityUser user;

    @Mock
    private Stack stack;

    @Mock
    private AmbariHaComponentFilter ambariHaComponentFilter;

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
        when(request.getConnectedCluster()).thenReturn(mock(ConnectedClusterRequest.class));
        when(sharedServiceConfigProvider.configureCluster(any(Cluster.class), any(ConnectedClusterRequest.class)))
                .thenReturn(expectedClusterInstance);
        when(clusterProxyDecorator.prepareProxyConfig(any(Cluster.class), any(), any(Stack.class))).thenReturn(expectedClusterInstance);
        when(ambariHaComponentFilter.getHaComponents(any())).thenReturn(Collections.emptySet());

        Cluster result = underTest.decorate(expectedClusterInstance, request, blueprint, new Organization(), stack);

        Assert.assertEquals(expectedClusterInstance, result);
        verify(sharedServiceConfigProvider, times(1)).configureCluster(any(Cluster.class),
                any(ConnectedClusterRequest.class));
    }

}