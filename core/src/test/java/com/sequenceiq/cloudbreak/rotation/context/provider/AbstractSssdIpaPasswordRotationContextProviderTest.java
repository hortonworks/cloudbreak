package com.sequenceiq.cloudbreak.rotation.context.provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.mockito.Mock;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.SssdConfigProvider;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;

public abstract class AbstractSssdIpaPasswordRotationContextProviderTest {

    @Mock
    private SssdConfigProvider sssdConfigProvider;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private KerberosConfigService kerberosConfigService;

    protected void mockGetSssdPillar() {
        when(kerberosConfigService.get(any(), any())).thenReturn(Optional.of(new KerberosConfig()));
        when(clusterHostServiceRunner.getServiceLocations(any())).thenReturn(Map.of());
        when(clusterComponentConfigProvider.getCdhProduct(any())).thenReturn(Optional.of(new ClouderaManagerProduct()));
        when(sssdConfigProvider.createSssdIpaPillar(any(), any(), any(), any())).thenReturn(Map.of("sssd",
                new SaltPillarProperties("path", Map.of("pass", "any"))));
    }
}
