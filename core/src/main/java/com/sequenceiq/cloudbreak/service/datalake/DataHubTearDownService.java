package com.sequenceiq.cloudbreak.service.datalake;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.datalake.DatalakeDto;
import com.sequenceiq.cloudbreak.sdx.paas.LocalPaasDhTearDownService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Service
public class DataHubTearDownService implements LocalPaasDhTearDownService {

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Override
    public void tearDownDataHub(String sdxCrn, String datahubCrn) {
        Stack stack = stackService.getByCrnWithLists(datahubCrn);
        ClusterApi clusterApi = clusterApiConnectors.getConnector(stack);
        Optional<DatalakeDto> datalakeDto = Optional.empty();
        Optional<Stack> dataLakeOptional = Optional.ofNullable(stackService.getByCrnOrElseNull(sdxCrn));
        if (dataLakeOptional.isPresent()) {
            Stack dataLake = dataLakeOptional.get();
            HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(dataLake.getId(),
                    dataLake.getClusterManagerIp(), dataLake.cloudPlatform());
            datalakeDto = Optional.of(
                    DatalakeDto.DatalakeDtoBuilder.aDatalakeDto()
                            .withGatewayPort(dataLake.getGatewayPort())
                            .withHttpClientConfig(httpClientConfig)
                            .withPassword(dataLake.getCluster().getCloudbreakAmbariPassword())
                            .withUser(dataLake.getCluster().getCloudbreakAmbariUser())
                            .withName(dataLake.getName())
                            .withStatus(dataLake.getStatus())
                            .build()
            );
        }
        clusterApi.clusterSecurityService().deregisterServices(stack.getName(), datalakeDto);
    }
}
