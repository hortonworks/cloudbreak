package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.SERVICES_RUNNING;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.view.GatewayView;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class ClusterServiceRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterServiceRunner.class);

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private ClusterHostServiceRunner hostRunner;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private GatewayService gatewayService;

    public void runClusterManagerServices(StackDto stackDto, boolean runPreServiceDeploymentRecipe) {
        ClusterView cluster = stackDto.getCluster();

        generateGatewaySignKeys(stackDto.getGateway());

        MDCBuilder.buildMdcContext(cluster);
        hostRunner.runClusterServices(stackDto, Map.of(), runPreServiceDeploymentRecipe);
        List<Long> instanceIds = stackDto.getRunningInstanceMetaDataSet().stream().map(InstanceMetadataView::getId).toList();
        instanceMetaDataService.updateInstanceStatuses(instanceIds, SERVICES_RUNNING, null);

    }

    private void generateGatewaySignKeys(GatewayView gateway) {
        if (Objects.nonNull(gateway)) {
            gatewayService.generateAndUpdateSignKeys(gateway);
        }
    }

    public String updateClusterManagerClientConfig(StackDto stackDto) {
        String gatewayIp = gatewayConfigService.getPrimaryGatewayIp(stackDto);
        if (!gatewayIp.equals(stackDto.getClusterManagerIp())) {
            LOGGER.debug("Cluster manager IP has changed from {} to {}, updating cluster metadata.", stackDto.getClusterManagerIp(), gatewayIp);
            HttpClientConfig clusterManagerClientConfig = buildClusterManagerClientConfig(stackDto.getStack(), gatewayIp);
            clusterService.updateClusterManagerClientConfig(stackDto.getCluster().getId(), clusterManagerClientConfig);
        }
        return gatewayIp;
    }

    public void redeployGatewayCertificate(Long stackId) {
        StackDto stackDto = stackDtoService.getById(stackId);
        hostRunner.redeployGatewayCertificate(stackDto);
    }

    public void redeployGatewayPillar(Long stackId) {
        StackDto stack = stackDtoService.getById(stackId);
        hostRunner.redeployGatewayPillarOnly(stack, Set.of());
    }

    public void redeployStates(Long stackId) {
        StackDto stack = stackDtoService.getById(stackId);
        hostRunner.redeployStates(stack);
    }

    public String changePrimaryGateway(Long stackId) throws CloudbreakException {
        StackDto stackDto = stackDtoService.getById(stackId);
        return hostRunner.changePrimaryGateway(stackDto);
    }

    private HttpClientConfig buildClusterManagerClientConfig(StackView stack, String gatewayPublicIp) {
        return tlsSecurityService.buildTLSClientConfigForPrimaryGateway(stack.getId(), gatewayPublicIp, stack.getCloudPlatform());
    }
}
