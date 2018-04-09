package com.sequenceiq.cloudbreak.service.decorator;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.converter.mapper.AmbariDatabaseMapper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariConfigurationService;
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
    private BlueprintValidator blueprintValidator;

    @Mock
    private StackService stackService;

    @Mock
    private ConversionService conversionService;

    @Mock
    private HostGroupDecorator hostGroupDecorator;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private LdapConfigValidator ldapConfigValidator;

    @Mock
    private ClusterService clusterService;

    @Mock
    private RdsConnectionValidator rdsConnectionValidator;

    @Mock
    private ClusterProxyDecorator clusterProxyDecorator;

    @Mock
    private AmbariConfigurationService ambariConfigurationService;

    @Mock
    private AmbariDatabaseMapper ambariDatabaseMapper;

    @Mock
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Mock
    private Cluster subject;

    @Mock
    private ClusterRequest request;

    @Mock
    private Blueprint blueprint;

    @Mock
    private IdentityUser user;

    @Mock
    private Stack stack;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDecorateIfMethodCalledThenSharedServiceConfigProviderShouldBeCalledOnceToConfigureTheCluster() {
        Cluster expectedClusterInstance = new Cluster();
        when(sharedServiceConfigProvider.configureCluster(any(Cluster.class), any(IdentityUser.class), any(ConnectedClusterRequest.class)))
                .thenReturn(expectedClusterInstance);
        when(ambariConfigurationService.createDefaultRdsConfigIfNeeded(any(Stack.class), any(Cluster.class))).thenReturn(Optional.empty());
        when(clusterProxyDecorator.prepareProxyConfig(any(Cluster.class), any(IdentityUser.class), anyString(), any(Stack.class))).thenReturn(subject);

        Cluster result = underTest.decorate(subject, request, blueprint, user, stack);

        Assert.assertEquals(expectedClusterInstance, result);
        verify(sharedServiceConfigProvider, times(1)).configureCluster(any(Cluster.class), any(IdentityUser.class),
                any(ConnectedClusterRequest.class));
    }

}