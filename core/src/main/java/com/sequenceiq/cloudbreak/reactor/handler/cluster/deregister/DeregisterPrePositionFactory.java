package com.sequenceiq.cloudbreak.reactor.handler.cluster.deregister;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.dto.datalake.DatalakeDto;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class DeregisterPrePositionFactory {

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackService stackService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    public ClusterApi clusterApi(StackDtoDelegate stack) {
        return clusterApiConnectors.getConnector(stack);
    }

    public Optional<DatalakeDto> datalakeDto(StackView stack) {
        Optional<DatalakeDto> datalakeDto = Optional.empty();
        Optional<Stack> dataLakeOptional = Optional.ofNullable(stackService.getByCrnOrElseNull(stack.getDatalakeCrn()));
        if (dataLakeOptional.isPresent()) {
            Stack dataLake = dataLakeOptional.get();
            HttpClientConfig httpClientConfig = tlsSecurityService.buildTLSClientConfigForPrimaryGateway(dataLake.getId(),
                    dataLake.getClusterManagerIp(), dataLake.cloudPlatform());
            datalakeDto = Optional.ofNullable(
                    DatalakeDto.DatalakeDtoBuilder.aDatalakeDto()
                            .withGatewayPort(dataLake.getGatewayPort())
                            .withHttpClientConfig(httpClientConfig)
                            .withPassword(dataLake.getCluster().getCloudbreakClusterManagerPassword())
                            .withUser(dataLake.getCluster().getCloudbreakClusterManagerUser())
                            .withName(dataLake.getName())
                            .withStatus(dataLake.getStatus())
                            .build()
            );
        }
        return datalakeDto;
    }

}
