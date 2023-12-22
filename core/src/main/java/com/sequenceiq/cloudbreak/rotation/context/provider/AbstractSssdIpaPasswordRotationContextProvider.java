package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.SssdConfigProvider;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;

public abstract class AbstractSssdIpaPasswordRotationContextProvider {

    @Inject
    private SssdConfigProvider sssdConfigProvider;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private KerberosConfigService kerberosConfigService;

    protected Map<String, com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties> getSssdIpaPillar(StackDto stackDto) {
        KerberosConfig kerberosConfig = kerberosConfigService.get(stackDto.getEnvironmentCrn(), stackDto.getName()).orElseThrow();
        Map<String, List<String>> serviceLocations = clusterHostServiceRunner.getServiceLocations(stackDto);
        Optional<ClouderaManagerProduct> cdhProduct = clusterComponentConfigProvider.getCdhProduct(stackDto.getCluster().getId());
        return sssdConfigProvider.createSssdIpaPillar(kerberosConfig, serviceLocations, stackDto.getEnvironmentCrn(), cdhProduct);
    }
}
